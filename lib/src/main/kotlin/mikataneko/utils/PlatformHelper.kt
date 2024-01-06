package mikataneko.utils

fun getPlatform(): Platform {
    val os = System.getProperty("os.name").toString().lowercase()
    return with(os) {
        when {
            contains("mac") -> Platform.OSX
            contains("window") -> Platform.WINDOWS
            contains("linux") -> Platform.LINUX
            else -> Platform.UNIVERSAL
        }
    }
}

fun getArch(): Arch {
    val arch = System.getProperty("os.arch").toString().lowercase()
    return with(arch) {
        when {
            contains("aarch64") || contains("arm64") -> Arch.ARM64
            contains("x86") -> Arch.X86
            contains("x64") -> Arch.X64
            else -> Arch.UNIVERSAL
        }
    }
}

enum class Platform(val detail:String,val key: String) {
    WINDOWS("windows","windows"),
    OSX("osx","macos"),
    LINUX("linux","linux"),
    UNIVERSAL("",""),
}

enum class Arch(val detail: String, val type: String) {
    ARM64("-arm64", "64"),
    X86("-x86", "32"),
    X64("","64"),
    UNIVERSAL("","")
}
