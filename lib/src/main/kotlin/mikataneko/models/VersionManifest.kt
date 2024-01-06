package mikataneko.models


import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class VersionManifest(
    val latest: Latest,
    val versions: List<Version>,
) {
    fun version(id: String): Version? =
        this.versions.find { it.id == id }

    val latestRelease =
        this.version(this.latest.release)!!

    val latestSnapshot =
        this.version(this.latest.snapshot)!!
}

@Serializable
data class Version(
    val id: String,
    val releaseTime: String,
    val time: String,
    val type: String,
    val url: String,
)

@Serializable
data class Latest(
    val release: String,
    val snapshot: String,
)
