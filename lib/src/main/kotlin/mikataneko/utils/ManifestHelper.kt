package mikataneko.utils

import io.ktor.client.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import mikataneko.models.GameInstance
import mikataneko.models.*
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.exists

class ManifestHelper(
    private val client: HttpClient,
    private val retries: Int,
    private val server: String = "https://piston-meta.mojang.com",
) {

    private val downloader = Downloader(client = client)

    suspend fun getLatestVersionManifest(): VersionManifest {
        val response = client.get("$server/mc/game/version_manifest.json")
        val manifest = Json.decodeFromString<VersionManifest>(response.bodyAsText())
        return manifest
    }

    private fun loadVersionManifest(path: Path): MinecraftVersionManifest {
        val content = Files.readString(path)
        return run { Json.decodeFromString<MinecraftVersionManifest>(content) }

    }

    private suspend fun getVersionManifest(instance: GameInstance): MinecraftVersionManifest {
        val path = instance.rootDirectory.versions.resolve(instance.id).resolve("${instance.id}.json")
        return withContext(Dispatchers.IO) {
            if (!path.exists()) {
                downloader.getAndSaveVersionManifest(instance)
            }
            val manifest = loadVersionManifest(path)
            manifest
        }
    }

    private fun loadAssetIndexes(path: Path): AssetIndexes {
        val content = Files.readString(path)
        return run {
            Json.decodeFromString<AssetIndexes>(content)
        }
    }

    private suspend fun getAssetIndex(instance: GameInstance): AssetIndexes {
        val manifest = getVersionManifest(instance)
        val path = instance.rootDirectory.assetIndexes.resolve("${manifest.assetIndex.id}.json")
        return withContext(Dispatchers.IO) {
            if (!path.exists() || path.sha1() != manifest.assetIndex.sha1) {
                downloader.getAndSaveAssetIndex(manifest, instance.rootDirectory)
            }
            loadAssetIndexes(path)
        }
    }

    suspend fun downloadObjects(instance: GameInstance, parallel: Int) {
        val semaphore = Semaphore(parallel)
        val index = getAssetIndex(instance)
        withContext(Dispatchers.IO) {
            index.objects.map {
                async {
                    semaphore.withPermit {
                        val resolveValue = it.value.hash.take(2) + "/" + it.value.hash
                        val path = instance.rootDirectory.objects.resolve(resolveValue)
                        if (!path.exists()) {
                            retryOnException<HashVerificationFailedException>(retries) {
                                downloader.getAndSaveObject(it.value, instance.rootDirectory)
                            }
                        }
                    }
                }
            }.awaitAll()
        }
    }

    suspend fun verifyObjects(instance: GameInstance, parallel: Int) {
        val semaphore = Semaphore(parallel)
        val index = getAssetIndex(instance)
        withContext(Dispatchers.IO) {
            index.objects.map {
                async {
                    semaphore.withPermit {
                        val resolveValue = it.value.hash.take(2) + "/" + it.value.hash
                        val file = instance.rootDirectory.objects.resolve(resolveValue)
                        if (!file.exists() || file.sha1() != it.value.hash) {
                            retryOnException<HashVerificationFailedException>(retries) {
                                downloader.getAndSaveObject(it.value, instance.rootDirectory)
                            }
                        }
                    }
                }
            }.awaitAll()
        }
    }

    suspend fun verifyAndDownloadClientJar(instance: GameInstance) {
        val path = instance.rootDirectory.versions.resolve(instance.id).resolve("${instance.id}.jar")
        if (!path.exists() || path.sha1() != getVersionManifest(instance).downloads.client.sha1) {
            downloader.getAndSaveClientJar(getVersionManifest(instance), instance.id, instance.rootDirectory)
        }
    }

    suspend fun downloadLibraries(instance: GameInstance, parallel: Int) {
        val resolvedLibraries = getVersionManifest(instance).libraries.mapNotNull(Library::resolve)
        val semaphore = Semaphore(parallel)
        withContext(Dispatchers.IO) {
            resolvedLibraries.map {
                async {
                    semaphore.withPermit {
                        retryOnException<HashVerificationFailedException>(retries) {
                            downloader.getAndSaveLibrary(it, instance.id, instance.rootDirectory)
                        }
                    }
                }
            }.awaitAll()
        }
    }
}

inline fun <reified T : Exception> retryOnException(retries: Int, block: () -> Unit) {
    var retry = 0
    while (retry <= retries) {
        try {
            block()
            break
        } catch (e: Exception) {
            if (e is T) {
                retry++
                continue
            } else {
                throw e
            }
        }
    }
}
