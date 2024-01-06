package mikataneko.models

import kotlinx.serialization.Serializable

@Serializable
data class AssetIndexes(
    val objects: Map<String, Object>
)

@Serializable
data class Object(
    val hash: String,
    val size: Long
)
