package mikataneko.utils

import java.nio.file.Files
import java.nio.file.Path
import java.util.HexFormat
import kotlin.io.path.readBytes

data class GameDirectory(
    val path: Path,
    val versions: Path = path.resolve("versions"),
    val assets: Path = path.resolve("assets"),
    val libraries: Path = path.resolve("libraries"),
    val assetIndexes: Path = assets.resolve("indexes"),
    val objects: Path = assets.resolve("objects"),
    val legacy: Path = assets.resolve("virtual/legacy"),
    val natives: Path = path.resolve("natives"),
)

fun loadGameDirectory(path: Path): GameDirectory {
    val directory = newGameDirectory(path)
    createFolders(directory.path)
    return directory
}

fun newGameDirectory(parentDirectory: Path): GameDirectory {
    val directoryPath = parentDirectory.resolve(".minecraft")
    if (!Files.exists(directoryPath)) {
        Files.createDirectory(directoryPath)
    }
    return GameDirectory(directoryPath)
}

fun createFolders(path: Path) {
    val folders = listOf("assets", "libraries", "versions", "assets/indexes", "assets/objects", "assets/virtual/legacy")

    folders.forEach {
        val folder = path.resolve(it)
        if (!Files.exists(folder)) {
            Files.createDirectories(folder)
        }
    }
}

fun Path.sha1(): String? = HexFormat.of().formatHex(io.ktor.util.sha1(this.readBytes()))
