package mikataneko.models.minecraft

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject

sealed class ProfileResponse

@Serializable
data class FoundProfile(
    val id: String,
    val name: String,
    val skins: List<Skin>,
    val capes: List<Cape>,
    val profileActions: JsonObject,
) : ProfileResponse()

@Serializable
data class Skin(
    val id: String,
    val state: String,
    val url: String,
    val textureKey: String,
    val variant: String,
)

@Serializable
data class Cape(
    val id: String,
    val state: String,
    val url: String,
    val alias: String,
)

@Serializable
data class NotFoundProfile(
    val path: String,
    val error: String,
    val errorMessage: String,
) : ProfileResponse()
