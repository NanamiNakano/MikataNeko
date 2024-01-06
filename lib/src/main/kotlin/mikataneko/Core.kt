package mikataneko

import mikataneko.models.GameInstance
import mikataneko.utils.GameDirectory
import mikataneko.utils.ManifestHelper
import mikataneko.utils.loadGameDirectory
import java.nio.file.Paths

class Core(val gameDirectory: GameDirectory = loadGameDirectory(Paths.get("").toAbsolutePath())) {
    val manifestHelper = ManifestHelper()
}
