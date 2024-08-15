package xyz.kgy_production.webdavebookmanager.data.model

import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import xyz.kgy_production.webdavebookmanager.util.serializer.LocalDateTimeSerializer
import java.time.LocalDateTime

@Serializable
data class WebDavDirNode(
    val current: String,
    val relativePath: String? = null,
    val children: List<String> = listOf(),
    @Serializable(with = LocalDateTimeSerializer::class)
    val lastUpdated: LocalDateTime,
) {
    companion object {
        fun fromJson(json: String): WebDavDirNode {
            val node = Json.decodeFromString<WebDavDirNode>(json)
            return WebDavDirNode(
                current = node.current,
                relativePath = node.relativePath,
                children = node.children,
                lastUpdated = node.lastUpdated
            )
        }
    }

    fun toJson(): String {
        return Json.encodeToString(
            WebDavDirNode(
                current = current,
                relativePath = relativePath,
                children = children,
                lastUpdated = lastUpdated
            )
        )
    }
}
