package mikataneko.models.minecraft

import kotlinx.serialization.Serializable

@Serializable
data class GameItems(
    val items: List<GameItem>,
    val signature: String,
    val keyId: String,
)

@Serializable
data class GameItem(
    val name: String,
    val signature: String,
)
