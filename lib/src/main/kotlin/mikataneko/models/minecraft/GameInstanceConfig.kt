package mikataneko.models.minecraft

data class GameInstanceConfig(
    val isDemoUser: Boolean = false,
    val hasCustomResolution: Boolean = false,
    val customResolution: Pair<Int, Int> = Pair(0, 0),
    val hasQuickPlaysSupport: Boolean = false,
    val quickPlayPath: String = "",
    val isQuickPlaySingleplayer: Boolean = false,
    val quickPlaySingleplayer: String = "",
    val isQuickPlayMultiplayer: Boolean = false,
    val quickPlayMultiplayer: String = "",
    val isQuickPlayRealms: Boolean = false,
    val quickPlayRealms: String = "",
)
