package mikataneko.models

class MicrosoftAuthenticationProvider(
    val clientId: String,
    val tenantId: String = "common",
    val scopes: List<String> = listOf("XboxLive.signin", "offline_access", "openid", "profile", "email"),
)

