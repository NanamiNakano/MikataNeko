package mikataneko.models.minecraft

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject


sealed class MinecraftAuthenticate

@Serializable
data class XboxMinecraftAuthenticate(
    val identityToken: String,
) : MinecraftAuthenticate()

@Serializable
data class MinecraftAuthenticateResponse(
    val username: String,
    val roles: JsonElement,
    val metadata: JsonObject,
    @SerialName("access_token")
    val accessToken: String,
    @SerialName("token_type")
    val tokenType: String,
    @SerialName("expires_in")
    val expiresIn: Int,
)
