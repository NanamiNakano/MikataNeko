package mikataneko.models.xbox


import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import java.time.Instant

@Serializable
data class XboxAuthenticateResponse(
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
)

@Serializable
data class DisplayClaims(
    val xui: List<Map<String, String>>,
)

object InstantSerializer : KSerializer<Instant> {
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("java.time.Instant", PrimitiveKind.STRING)
    override fun serialize(encoder: Encoder, value: Instant) = encoder.encodeString(value.toString())
    override fun deserialize(decoder: Decoder): Instant = Instant.parse(decoder.decodeString())
}
