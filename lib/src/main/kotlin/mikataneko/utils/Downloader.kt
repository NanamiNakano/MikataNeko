package mikataneko.utils

import io.ktor.client.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.util.*
import io.ktor.utils.io.core.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import mikataneko.models.GameInstance
import mikataneko.models.*
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption
import java.util.*
import java.util.jar.JarFile
import kotlin.io.path.Path
import kotlin.io.path.appendBytes
import kotlin.io.path.deleteExisting
import kotlin.text.toByteArray

class Downloader(
    private val client: HttpClient,
    private val server: String = "https://resources.download.minecraft.net",
) {
    suspend fun getAndSaveVersionManifest(instance: GameInstance): MinecraftVersionManifest {
        val path = instance.rootDirectory.versions.resolve(instance.id).resolve("${instance.id}.json")
        val response = withContext(Dispatchers.IO) {
            client.get(instance.version.url)
        }

        val body = response.bodyAsText()
        val minecraftVersion = Json.decodeFromString<MinecraftVersionManifest>(body)
        withContext(Dispatchers.IO) {
            Files.createDirectories(path.parent)
            Files.write(path, body.toByteArray())
        }
        return minecraftVersion
    }

    suspend fun getAndSaveAssetIndex(manifest: MinecraftVersionManifest, rootDirectory: GameDirectory): AssetIndexes {
        val path = rootDirectory.assetIndexes.resolve("${manifest.assetIndex.id}.json")
        val response = withContext(Dispatchers.IO) {
            client.get(manifest.assetIndex.url)
        }

        val body = response.bodyAsText()
        if (HexFormat.of().formatHex(sha1(body.toByteArray())) != manifest.assetIndex.sha1) {
            throw HashVerificationFailedException("${manifest.assetIndex.id}.json")
        }
        val assetIndexes = Json.decodeFromString<AssetIndexes>(body)
        withContext(Dispatchers.IO) {
            Files.write(path, body.toByteArray())
        }
        return assetIndexes
    }

    suspend fun getAndSaveObject(target: Object, rootDirectory: GameDirectory) {
        val resolveValue = target.hash.take(2) + "/" + target.hash
        val url = Url(server).toURI().resolve(resolveValue).toString()
        withContext(Dispatchers.IO) {
            val tempFile = async { client.downloadFromUrl(url) }.await()

            if (tempFile.sha1() != target.hash) {
                throw HashVerificationFailedException("object: ${target.hash}")
            }

            Files.createDirectories(rootDirectory.objects.resolve(target.hash.take(2)))
            Files.createDirectories(rootDirectory.legacy.resolve(target.hash.take(2)))
            Files.copy(tempFile, rootDirectory.objects.resolve(resolveValue), StandardCopyOption.REPLACE_EXISTING)
            Files.copy(tempFile, rootDirectory.legacy.resolve(resolveValue), StandardCopyOption.REPLACE_EXISTING)
            tempFile.deleteExisting()
        }
    }

    suspend fun getAndSaveClientJar(manifest: MinecraftVersionManifest, id: String, rootDirectory: GameDirectory) {
        val path = rootDirectory.versions.resolve(id).resolve("${id}.jar")
        val target = manifest.downloads.client
        withContext(Dispatchers.IO) {
            downloadSingleFile(target, path)
        }
    }

    suspend fun getAndSaveLibrary(lib: ResolvedLibrary, id: String, rootDirectory: GameDirectory) {
        when (lib) {
            is NativeLibrary -> {
                withContext(Dispatchers.IO) {
                    val path = rootDirectory.versions.resolve(id).resolve("natives")
                    Files.createDirectories(path)
                    val tempFile = async { client.downloadFromUrl(lib.url) }.await()

                    if (tempFile.sha1() != lib.sha1) {
                        throw HashVerificationFailedException("library: ${lib.name}")
                    }

                    JarFile(tempFile.toFile()).use { jarFile ->
                        jarFile.entries().asSequence().forEach {
                            if (it.name.endsWith(".dll") || it.name.endsWith(".dylib") || it.name.endsWith(".so")) {
                                jarFile.getInputStream(it).use { entryInputStream ->
                                    Files.copy(
                                        entryInputStream,
                                        path.resolve(Path(it.name).fileName),
                                        StandardCopyOption.REPLACE_EXISTING
                                    )
                                }
                            }
                        }
                    }
                    tempFile.deleteExisting()
                }
            }

            is UniversalLibrary -> {
                withContext(Dispatchers.IO) {
                    val path = rootDirectory.libraries.resolve(lib.path)
                    Files.createDirectories(path.parent)
                    val tempFile = client.downloadFromUrl(lib.url)

                    if (tempFile.sha1() != lib.sha1) {
                        throw HashVerificationFailedException("library: ${lib.name}")
                    }

                    Files.copy(tempFile, path, StandardCopyOption.REPLACE_EXISTING)
                    tempFile.deleteExisting()
                }
            }
        }
    }

    suspend fun downloadSingleFile(file: FileDownload, savePath: Path) {
        withContext(Dispatchers.IO) {
            val tempFile = client.downloadFromUrl(file.url)

            if (tempFile.sha1() != file.sha1) {
                throw HashVerificationFailedException("failed to verify: ${file.url}")
            }

            Files.createDirectories(savePath.parent)
            Files.copy(tempFile, savePath, StandardCopyOption.REPLACE_EXISTING)
            tempFile.toFile().deleteOnExit()
        }

    }
}

private suspend fun HttpClient.downloadFromUrl(url: String): Path {
    return withContext(Dispatchers.IO) {
        val tempFile = Files.createTempFile("mikataneko-", "")

        this@downloadFromUrl.prepareGet(url).execute { response: HttpResponse ->
            val channel = response.bodyAsChannel()
            while (!channel.isClosedForRead) {
                val packet = channel.readRemaining(DEFAULT_BUFFER_SIZE.toLong())
                while (!packet.isEmpty) {
                    val bytes = packet.readBytes()
                    tempFile.appendBytes(bytes)
                }
            }
        }
        tempFile
    }
}
