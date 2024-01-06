package mikataneko.models

import io.ktor.http.content.*
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.buildClassSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.*
import mikataneko.utils.Arch
import mikataneko.utils.Platform
import mikataneko.utils.getArch
import mikataneko.utils.getPlatform

@Serializable
data class MinecraftVersionManifest(
    val arguments: Arguments? = null,
    val assetIndex: AssetIndex,
    val assets: String,
    val complianceLevel: Int,
    val downloads: Downloads,
    val id: String,
    val javaVersion: JavaVersion,
    val libraries: List<Library>,
    val logging: Logging,
    val mainClass: String,
    val minecraftArguments: String? = null,
    val minimumLauncherVersion: Int,
    val releaseTime: String,
    val time: String,
    val type: String,
)

@Serializable
data class Arguments(
    val game: List<GameArgument>,
    val jvm: List<JvmArgument>,
)

object GameArgumentSerializer : JsonContentPolymorphicSerializer<GameArgument>(GameArgument::class) {
    override fun selectDeserializer(element: JsonElement) = when {
        element is JsonPrimitive -> StringValue.serializer()
        else -> GameArgumentClass.serializer()
    }
}

object StringValueSerializer : KSerializer<StringValue> {
    override val descriptor: SerialDescriptor = buildClassSerialDescriptor("StringValue")

    override fun deserialize(decoder: Decoder): StringValue {
        require(decoder is JsonDecoder)
        val element = decoder.decodeJsonElement()
        return StringValue(element.jsonPrimitive.content)
    }

    override fun serialize(encoder: Encoder, value: StringValue) {
        encoder.encodeString(value.value)
    }
}

object StringOrArraySerializer : JsonTransformingSerializer<List<String>>(ListSerializer(String.serializer())) {
    override fun transformDeserialize(element: JsonElement): JsonElement =
        if (element !is JsonArray) JsonArray(listOf(element)) else element
}

@Serializable(with = GameArgumentSerializer::class)
sealed class GameArgument

@Serializable(with = StringValueSerializer::class)
class StringValue(val value: String) : GameArgument()


@Serializable
data class GameArgumentClass(
    val rules: List<Rule>? = null,
    @Serializable(with = StringOrArraySerializer::class)
    val value: List<String>? = null,
) : GameArgument()


@Serializable
data class Rule(
    val action: String,
    val features: Map<String, Boolean>? = null,
    val os: Os? = null,
)

@Serializable
data class Os(
    val name: String? = null,
    val arch: String? = null,
    val version: String? = null,
)

object JvmArgumentSerializer : JsonContentPolymorphicSerializer<JvmArgument>(JvmArgument::class) {
    override fun selectDeserializer(element: JsonElement) = when {
        element is JsonPrimitive -> StringArgument.serializer()
        else -> JvmArgumentClass.serializer()
    }
}

object StringArgumentSerializer : KSerializer<StringArgument> {
    override val descriptor: SerialDescriptor = buildClassSerialDescriptor("StringValue")

    override fun deserialize(decoder: Decoder): StringArgument {
        require(decoder is JsonDecoder)
        val element = decoder.decodeJsonElement()
        return StringArgument(element.jsonPrimitive.content)
    }

    override fun serialize(encoder: Encoder, value: StringArgument) {
        encoder.encodeString(value.value)
    }
}

@Serializable(with = JvmArgumentSerializer::class)
sealed class JvmArgument

@Serializable
data class JvmArgumentClass(
    val rules: List<Rule>? = null,
    val value: JsonElement,
) : JvmArgument()

@Serializable(with = StringArgumentSerializer::class)
class StringArgument(val value: String) : JvmArgument()

@Serializable
data class AssetIndex(
    val id: String,
    val sha1: String,
    val size: Long,
    val totalSize: Long,
    val url: String,
)

@Serializable
data class Downloads(
    val client: FileDownload,
    @SerialName("client_mappings")
    val clientMappings: FileDownload? = null,
    val server: FileDownload,
    @SerialName("server_mappings")
    val serverMappings: FileDownload? = null,
    @SerialName("windows_server")
    val windowsServer: FileDownload? = null,
)

@Serializable
data class FileDownload(
    val id: String? = null,
    val path: String? = null,
    val sha1: String,
    val size: Long,
    val url: String,
)

@Serializable
data class JavaVersion(
    val component: String,
    val majorVersion: Int,
)

@Serializable
data class Extract(
    val exclude: List<String>,
)

@Serializable
data class Library(
    val downloads: LibraryDownloads,
    val name: String,
    val extract: Extract? = null,
    val natives: Map<String, String>? = null,
    val rules: List<Rule>? = null,
) {
    fun resolve(): ResolvedLibrary? {
        val os = getPlatform()
        val arch = getArch()

        var allowed = true

        rules?.forEach {
            val ruleOS = (it.os ?: return null).name

            if (ruleOS == null || ruleOS == os.detail) {
                when (it.action) {
                    "disallow" -> allowed = false
                    "allow" -> allowed = true
                }
            }
        }
        if (!allowed) return null

        if (natives != null) {
            if (!natives.containsKey(os.detail)) {
                return null
            }
            val classifierKey = natives[os.detail]!!.apply {
                this.replace("\${arch}", arch.type)
            }
            val classifier = downloads.classifiers?.get(classifierKey)!!
            return NativeLibrary(
                classifier.path!!,
                classifier.url,
                classifier.sha1,
                classifier.size,
                name
            )
        }

        val artifact = downloads.artifact!!
        val nativeName = "natives-" + os.key + arch.detail
        if (name.contains(nativeName)) {
            return NativeLibrary(
                artifact.path!!,
                artifact.url,
                artifact.sha1,
                artifact.size,
                name
            )
        }

        return UniversalLibrary(
            artifact.path!!,
            artifact.url,
            artifact.sha1,
            artifact.size,
            name
        )
    }
}

@Serializable
data class LibraryDownloads(
    val artifact: FileDownload? = null,
    val classifiers: Map<String, FileDownload>? = null,
)

@Serializable
data class Logging(
    val client: ClientLogging,
)

@Serializable
data class ClientLogging(
    val argument: String,
    val file: FileDownload,
    val type: String,
)
