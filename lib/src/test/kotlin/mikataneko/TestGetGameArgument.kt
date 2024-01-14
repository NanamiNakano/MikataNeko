package mikataneko

import mikataneko.models.GameInstance
import mikataneko.models.minecraft.FoundProfile
import mikataneko.models.minecraft.GameInstanceConfig
import mikataneko.models.minecraft.NotFoundProfile
import mikataneko.models.xbox.DeviceCodeFlow
import mikataneko.models.xbox.XSTSError
import mikataneko.models.xbox.XSTSSuccess
import mikataneko.utils.MicrosoftAuthenticator
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


            val deviceCodeFlow = DeviceCodeFlow(System.getenv("CLIENT_ID"))
            val tokenCacheAspect = TokenCacheAspectImpl()

            val authenticator = MicrosoftAuthenticator(tokenCacheAspect, deviceCodeFlow)
            val accounts = authenticator.getCachedAccounts()
            println(accounts)

            val account = if (accounts.isEmpty()) {
                null
            } else {
                accounts.first()
            }
            println(account)

            val result = if (account != null) {
                authenticator.authenticateWithDeviceCode(account) { deviceCode ->
                    println(deviceCode.message())
                }
            } else {
                authenticator.authenticateWithDeviceCode { deviceCode ->
                    println(deviceCode.message())
                }
            }

//    val result = authenticator.authenticateWithDeviceCode { deviceCode ->
//        println(deviceCode.message())
//    }

            val accessToken = result.accessToken()
            val xblResponse = authenticator.authenticateWithXboxLive(accessToken)
            val xblToken = xblResponse.token
            val userHash = xblResponse.displayClaims.xui.first().uhs
            val xstsToken = when (val xstsResponse = authenticator.authorizeWithXSTS(xblToken)) {
                is XSTSError -> return
                is XSTSSuccess -> xstsResponse.token
            }

            val minecraftResponse = authenticator.authenticateWithMinecraft(userHash, xstsToken)
            val xuid = minecraftResponse.username
            val minecraftToken = minecraftResponse.accessToken
            val profile = authenticator.getProfile(minecraftToken).let {
                when (it) {
                    is FoundProfile -> it
                    is NotFoundProfile -> return
                }
            }


            val argument = core.getArgument(instance, profile, minecraftToken, xuid, GameInstanceConfig())
            println(argument)
        }
    }
}
