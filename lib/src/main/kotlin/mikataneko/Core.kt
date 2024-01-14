package mikataneko

import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.serialization.kotlinx.json.*
import mikataneko.models.*
import mikataneko.models.minecraft.FoundProfile
import mikataneko.models.minecraft.GameInstanceConfig
import mikataneko.utils.*
import java.nio.file.Paths


val globalClient by lazy {
    HttpClient(CIO) {
        expectSuccess = true

        install(HttpRequestRetry) {
            retryOnExceptionOrServerErrors(maxRetries = 5)
            exponentialDelay()
        }

        install(HttpTimeout) {
            this.connectTimeoutMillis = 1000
            this.socketTimeoutMillis = 1000
        }

        install(ContentNegotiation) {
            json()
        }
    }
}

class Core(
    val gameDirectory: GameDirectory = loadGameDirectory(Paths.get("").toAbsolutePath()),
    client: HttpClient = globalClient,
    retries: Int = 5,
) {
    val manifestHelper = ManifestHelper(client, retries)

    suspend fun getArgument(
        instance: GameInstance,
        profile: FoundProfile,
        accessToken: String,
        xuid: String,
        config: GameInstanceConfig,
    ): String {
        val manifest = manifestHelper.getVersionManifest(instance)

        val gameArguments: MutableList<String> = mutableListOf()

        gameArguments.add("--username ${profile.name}")
        gameArguments.add("--version ${instance.id}")
        gameArguments.add("--gameDir ${instance.rootDirectory.path}")
        gameArguments.add("--assetsDir ${instance.rootDirectory.assets}")
        gameArguments.add("--assetIndex ${manifest.assetIndex.id}")
        gameArguments.add("--uuid ${profile.id}")
        gameArguments.add("--userType msa")
        gameArguments.add("--versionType ${manifest.type}")
        gameArguments.add("--xuid $xuid")
        gameArguments.add("--accessToken $accessToken")

        if (config.isDemoUser) {
            gameArguments.add("--demo")
        }
        if (config.hasCustomResolution) {
            gameArguments.add("--width ${config.customResolution.first} --height ${config.customResolution.second}")
        }
        if (config.hasQuickPlaysSupport) {
            gameArguments.add("--quickPlayPath ${config.quickPlayPath}")
        }
        if (config.isQuickPlaySingleplayer) {
            gameArguments.add("--quickPlaySingleplayer ${config.quickPlaySingleplayer}")
        }
        if (config.isQuickPlayMultiplayer) {
            gameArguments.add("--quickPlayMultiplayer ${config.quickPlayMultiplayer}")
        }
        if (config.isQuickPlayRealms) {
            gameArguments.add("--quickPlayRealms ${config.quickPlayRealms}")
        }

        val loggingArgument = manifest.logging.client.argument.replace(
            "\${path}", "${
                instance.rootDirectory.path.resolve(
                    manifest.logging.client.file.id!!
                )
            }"
        )

        if (manifest.arguments != null) {
            val jvmArgument = manifest.arguments.jvm.joinToString(" ") { argument ->
                when (argument) {
                    is JvmArgumentClass -> {
                        var allowed = false
                        val os = getPlatform()

                        argument.rules?.forEach {
                            val ruleOS = it.os?.name

                            if (ruleOS == null || ruleOS == os.detail) {
                                when (it.action) {
                                    "disallow" -> allowed = false
                                    "allow" -> allowed = true
                                }
                            }
                        }

                        if (!allowed) {
                            argument.value.joinToString(" ")
                        }

                        ""
                    }

                    is StringArgument -> argument.value
                }
            }
            return "$jvmArgument $loggingArgument ${manifest.mainClass} ${gameArguments.joinToString(" ")}"
        }

        return "$loggingArgument ${manifest.mainClass} ${gameArguments.joinToString(" ")}"
    }

}
