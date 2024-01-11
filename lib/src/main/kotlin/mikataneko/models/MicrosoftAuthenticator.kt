package mikataneko.models

import com.microsoft.aad.msal4j.*
import io.ktor.client.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import kotlinx.serialization.json.Json
import mikataneko.globalClient
import mikataneko.models.xbox.Properties
import mikataneko.models.xbox.XboxAuthenticate
import mikataneko.models.xbox.XboxAuthenticateResponse
import java.util.function.Consumer

data class DeviceCodeFlow(
    val clientId: String,
    val tenantId: String = "consumers",
    val scopes: List<String> = listOf("XboxLive.signin", "offline_access", "openid", "profile", "email"),
)

class MicrosoftAuthenticator(
    tokenCacheAspect: ITokenCacheAccessAspect,
    private val deviceCodeFlow: DeviceCodeFlow,
    private val client: HttpClient = globalClient,
) {
    private var pca: PublicClientApplication = PublicClientApplication.builder(deviceCodeFlow.clientId)
        .authority("https://login.microsoftonline.com/" + deviceCodeFlow.tenantId)
        .setTokenCacheAccessAspect(tokenCacheAspect).build()
        ?: throw MicrosoftAuthenticatorException("build public client application failed")

    fun getCachedAccounts(): Set<IAccount> {
        return pca.accounts.join()
    }

    fun authenticateWithDeviceCode(
        account: IAccount,
        deviceCodeHandler: (DeviceCode) -> Unit,
    ): IAuthenticationResult {
        try {
            val silentParameters: SilentParameters? =
                SilentParameters.builder(deviceCodeFlow.scopes.toSet(), account).build()
            val result: IAuthenticationResult? = pca.acquireTokenSilently(silentParameters).join()

            require(result != null) {
                throw MicrosoftAuthenticatorException("failed to acquire token")
            }

            return result
        } catch (ex: MsalException) {
            return authenticateWithDeviceCode { deviceCode -> deviceCodeHandler(deviceCode) }
        }
    }

    fun authenticateWithDeviceCode(
        deviceCodeHandler: (DeviceCode) -> Unit,
    ): IAuthenticationResult {
        val deviceCodeConsumer: Consumer<DeviceCode> = Consumer<DeviceCode> { deviceCode: DeviceCode ->
            deviceCodeHandler(deviceCode)
        }

        val parameters: DeviceCodeFlowParameters? =
            DeviceCodeFlowParameters.builder(deviceCodeFlow.scopes.toSet(), deviceCodeConsumer).build()
        val result: IAuthenticationResult? = pca.acquireToken(parameters).join()

        require(result != null) {
            throw MicrosoftAuthenticatorException("failed to acquire token")
        }

        return result
    }

    suspend fun authenticateWithXboxLive(token: String): XboxAuthenticateResponse {
        val response = client.post("https://user.auth.xboxlive.com/user/authenticate") {
            contentType(ContentType.Application.Json)
            setBody(XboxAuthenticate(Properties("d=$token")))
            accept(ContentType.Application.Json)
        }
        return Json.decodeFromString(response.bodyAsText())
    }
}

abstract class TokenCacheAspect : ITokenCacheAccessAspect {
    override fun beforeCacheAccess(iTokenCacheAccessContext: ITokenCacheAccessContext?) {
        val data = loadCacheData()
        iTokenCacheAccessContext?.tokenCache()?.deserialize(data)
    }

    override fun afterCacheAccess(iTokenCacheAccessContext: ITokenCacheAccessContext?) {
        val data = iTokenCacheAccessContext?.tokenCache()?.serialize()
        data?.let { saveCacheData(it) }
    }

    abstract fun loadCacheData(): String
    abstract fun saveCacheData(data: String)
}
