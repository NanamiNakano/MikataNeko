package mikataneko

import mikataneko.models.GameInstance
import kotlin.time.measureTime

suspend fun main() {
    val core = Core()
    run {
        with(core) {
            val versionManifest = manifestHelper.getLatestVersionManifest()
            val release = versionManifest.version("1.8.9")!!
            val instance = GameInstance(release, gameDirectory)
            val time = measureTime {
                manifestHelper.verifyObjects(instance, 64)
                manifestHelper.verifyAndDownloadClientJar(instance)
                manifestHelper.downloadLibraries(instance, 32)
                manifestHelper.downloadLoggingConfig(instance)
            }
            println(time)
        }
    }
}
