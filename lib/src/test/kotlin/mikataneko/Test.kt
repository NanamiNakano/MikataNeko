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

//fun main() {
//    run {
//        val provider = MicrosoftAuthenticationProvider("38253434-c68f-4d11-ac96-50efd9eb00d7")
//        MicrosoftAuthenticator(provider)
//    }
//}
