package mikataneko.models.xbox

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.time.Instant

sealed class XSTSAuthorizeResponse

@Serializable
data class XSTSError(
    @SerialName("Identity")
    val identity: String,
    @SerialName("XErr")
    val xErr: Int,
    @SerialName("Message")
    val message: String,
    @SerialName("Redirect")
    val redirect: String,
) : XSTSAuthorizeResponse()

@Serializable
data class XSTSSuccess(
    @SerialName("IssueInstant")
    @Serializable(with = InstantSerializer::class)
    val issueInstant: Instant,
    @SerialName("NotAfter")
    @Serializable(with = InstantSerializer::class)
    val notAfter: Instant,
    @SerialName("Token")
    val token: String,
    @SerialName("DisplayClaims")
    val displayClaims: DisplayClaims,
) : XSTSAuthorizeResponse()
