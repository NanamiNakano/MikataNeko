package mikataneko.models

sealed class ResolvedLibrary

data class UniversalLibrary(
    val path: String,
    val url:String,
    val sha1:String,
    val size:Long,
    val name:String,
):ResolvedLibrary()

data class NativeLibrary(
    val path: String,
    val url: String,
    val sha1: String,
    val size: Long,
    val name: String,
):ResolvedLibrary()
