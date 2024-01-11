package mikataneko

import mikataneko.models.DeviceCodeFlow
import mikataneko.models.MicrosoftAuthenticator
import mikataneko.models.TokenCacheAspect
import kotlin.io.path.readText
import kotlin.io.path.writeText

suspend fun main() {
    val deviceCodeFlow = DeviceCodeFlow(System.getenv("CLIENT_ID"))
    val tokenCacheAspect = TokenCacheAspectImpl()

    val authenticator = MicrosoftAuthenticator(tokenCacheAspect, deviceCodeFlow)
    val result = authenticator.authenticateWithDeviceCode { deviceCode ->
        println(deviceCode.message())
    }

    val token = result.accessToken()

    val response = authenticator.authenticateWithXboxLive(token)

    println(response.token)
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
