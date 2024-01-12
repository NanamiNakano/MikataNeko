package mikataneko.models.xbox

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class XSTSAuthorizeRequest(
    @SerialName("Properties")
    val xstsProperties: XSTSProperties,
    @SerialName("RelyingParty")
    val relyingParty: String = "rp://api.minecraftservices.com/",
    @SerialName("TokenType")
    val tokenType: String = "JWT",
)

@Serializable
data class XSTSProperties(
    @SerialName("UserTokens")
    val userTokens: List<String>,
    @SerialName("SandboxId")
    val sandboxId: String = "RETAIL",
)
