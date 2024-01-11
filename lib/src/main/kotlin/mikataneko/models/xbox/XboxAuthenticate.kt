package mikataneko.models.xbox


import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class XboxAuthenticate(
    @SerialName("Properties")
    val properties: Properties,
    @SerialName("RelyingParty")
    val relyingParty: String = "http://auth.xboxlive.com",
    @SerialName("TokenType")
    val tokenType: String = "JWT",
)

@Serializable
data class Properties(
    @SerialName("RpsTicket")
    val rpsTicket: String,
    @SerialName("AuthMethod")
    val authMethod: String = "RPS",
    @SerialName("SiteName")
    val siteName: String = "user.auth.xboxlive.com",
)
