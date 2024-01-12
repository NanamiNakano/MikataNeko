package mikataneko.utils

import com.microsoft.aad.msal4j.*
import io.ktor.client.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import kotlinx.serialization.json.Json
import mikataneko.globalClient
import mikataneko.models.MicrosoftAuthenticatorException
import mikataneko.models.minecraft.*
import mikataneko.models.xbox.*
import java.util.function.Consumer

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

    suspend fun authenticateWithXboxLive(accessToken: String): XboxAuthenticateResponse {
        val response = client.post("https://user.auth.xboxlive.com/user/authenticate") {
            contentType(ContentType.Application.Json)
            setBody(XboxAuthenticateRequest(XboxProperties("d=$accessToken")))
            accept(ContentType.Application.Json)
        }
        return Json.decodeFromString(response.bodyAsText())
    }

    suspend fun authorizeWithXSTS(xblToken: String): XSTSAuthorizeResponse {
        val response = client.post("https://xsts.auth.xboxlive.com/xsts/authorize") {
            contentType(ContentType.Application.Json)
            setBody(XSTSAuthorizeRequest(XSTSProperties(listOf(xblToken))))
            accept(ContentType.Application.Json)
        }

        if (response.status == HttpStatusCode.Unauthorized) {
            return Json.decodeFromString<XSTSError>(response.bodyAsText())
        }

        return Json.decodeFromString<XSTSSuccess>(response.bodyAsText())
    }

    suspend fun authenticateWithMinecraft(userHash: String, xstsToken: String): MinecraftAuthenticateResponse {
        val response = client.post("https://api.minecraftservices.com/authentication/login_with_xbox") {
            contentType(ContentType.Application.Json)
            setBody(XboxMinecraftAuthenticate("XBL3.0 x=$userHash;$xstsToken"))
            accept(ContentType.Application.Json)
        }

        return Json.decodeFromString(response.bodyAsText())
    }

    suspend fun getGameItems(minecraftAccessToken: String): GameItems {
        val response = client.get("https://api.minecraftservices.com/entitlements/mcstore") {
            header(HttpHeaders.Authorization, "Bearer $minecraftAccessToken")
        }

        return Json.decodeFromString(response.bodyAsText())
    }

    suspend fun getProfile(minecraftAccessToken: String): ProfileResponse {
        val response = client.get("https://api.minecraftservices.com/minecraft/profile") {
            header(HttpHeaders.Authorization, "Bearer $minecraftAccessToken")
        }

        if (response.status == HttpStatusCode.NotFound) {
            return Json.decodeFromString<NotFoundProfile>(response.bodyAsText())
        }

        return Json.decodeFromString<FoundProfile>(response.bodyAsText())
    }
}
