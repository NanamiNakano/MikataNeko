package mikataneko

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import mikataneko.models.xbox.DeviceCodeFlow
import mikataneko.utils.MicrosoftAuthenticator
import mikataneko.interfaces.TokenCacheAspect
import mikataneko.models.xbox.XSTSError
import mikataneko.models.xbox.XSTSSuccess
import kotlin.io.path.readText
import kotlin.io.path.writeText

private val json = Json {
    prettyPrint = true
}

suspend fun main() {
    val deviceCodeFlow = DeviceCodeFlow(System.getenv("CLIENT_ID"))
    val tokenCacheAspect = TokenCacheAspectImpl()

    val authenticator = MicrosoftAuthenticator(tokenCacheAspect, deviceCodeFlow)
    val result = authenticator.authenticateWithDeviceCode { deviceCode ->
        println(deviceCode.message())
    }

    val accessToken = result.accessToken()
    val xblResponse = authenticator.authenticateWithXboxLive(accessToken)
    val xblToken = xblResponse.token
    val userHash = xblResponse.displayClaims.xui.first().uhs
    val xstsToken = when (val xstsResponse = authenticator.authorizeWithXSTS(xblToken)) {
        is XSTSError -> return
        is XSTSSuccess -> xstsResponse.token
    }
    val minecraftToken = authenticator.authenticateWithMinecraft(userHash, xstsToken).accessToken
    val productList = authenticator.getGameItems(minecraftToken)
    println(json.encodeToString(productList))
}

class TokenCacheAspectImpl : TokenCacheAspect() {
    private val file = kotlin.io.path.createTempFile()
    override fun loadCacheData(): String {
        return file.readText()
    }

    override fun saveCacheData(data: String) {
        file.writeText(data)
        file.toFile().deleteOnExit()
    }
}
