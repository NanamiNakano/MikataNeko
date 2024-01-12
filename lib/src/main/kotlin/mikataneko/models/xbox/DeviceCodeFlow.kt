package mikataneko.models.xbox

data class DeviceCodeFlow(
    val clientId: String,
    val tenantId: String = "consumers",
    val scopes: List<String> = listOf("XboxLive.signin", "offline_access", "openid", "profile", "email"),
)
