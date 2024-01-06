package mikataneko.models

import mikataneko.models.MinecraftVersionManifest
import mikataneko.models.Version
import mikataneko.utils.GameDirectory
import mikataneko.utils.ManifestHelper
import mikataneko.utils.createFolders
import java.nio.file.Files
import java.nio.file.Path

class GameInstance(
    val version: Version,
    directory: GameDirectory,
    val id: String = version.id,
    private val isolation: Boolean = true,
) {
    var rootDirectory:GameDirectory
    init {
        if (isolation) {
            rootDirectory = GameDirectory(directory.versions.resolve(id),directory.versions)
            createFolders(directory.versions.resolve(id))
            Files.deleteIfExists(directory.versions.resolve(id).resolve("version"))
        } else {
            rootDirectory = directory
        }
    }
}
