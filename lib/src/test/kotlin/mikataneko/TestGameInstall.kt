package mikataneko

import mikataneko.models.GameInstance

suspend fun main() {
    val core = Core()
    run {
        with(core) {
            val versionManifest = manifestHelper.getLatestVersionManifest()
            val release = versionManifest.latestRelease
            val instance = GameInstance(release,gameDirectory)
            manifestHelper.verifyObjects(instance, 64)
            manifestHelper.verifyAndDownloadClientJar(instance)
            manifestHelper.downloadLibraries(instance, 32)
        }
    }
}
