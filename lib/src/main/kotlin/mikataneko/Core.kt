package mikataneko

import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.serialization.kotlinx.json.*
import mikataneko.utils.GameDirectory
import mikataneko.utils.ManifestHelper
import mikataneko.utils.loadGameDirectory
import java.nio.file.Paths


val globalClient by lazy {
    HttpClient(CIO) {
        expectSuccess = true

        install(HttpRequestRetry) {
            retryOnExceptionOrServerErrors(maxRetries = 5)
            exponentialDelay()
        }

        install(HttpTimeout) {
            this.connectTimeoutMillis = 1000
            this.socketTimeoutMillis = 1000
        }

        install(ContentNegotiation) {
            json()
        }
    }
}

class Core(
    val gameDirectory: GameDirectory = loadGameDirectory(Paths.get("").toAbsolutePath()),
    client: HttpClient = globalClient,
    retries: Int = 5,
) {
    val manifestHelper = ManifestHelper(client, retries)
}
