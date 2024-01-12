package mikataneko

import kotlinx.serialization.json.Json
import mikataneko.interfaces.TokenCacheAspect
import mikataneko.models.xbox.DeviceCodeFlow
import mikataneko.models.xbox.XSTSError
import mikataneko.models.xbox.XSTSSuccess
import mikataneko.utils.MicrosoftAuthenticator
import kotlin.io.path.*

suspend fun main() {
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
    val minecraftToken = authenticator.authenticateWithMinecraft(userHash, xstsToken).accessToken
    val profile = authenticator.getProfile(minecraftToken)
    println(profile)
}

class TokenCacheAspectImpl : TokenCacheAspect() {
    private val file = Path("./.mikataneko/tokenCacheAspectPersistence").apply {
        if (!this.exists()) {
            this.createParentDirectories().createFile()
        }
    }

    override fun loadCacheData(): String {
        return file.readText()
    }

    override fun saveCacheData(data: String) {
        file.writeText(data)
    }
}
