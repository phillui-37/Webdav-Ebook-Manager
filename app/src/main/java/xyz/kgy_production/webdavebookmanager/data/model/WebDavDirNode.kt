package xyz.kgy_production.webdavebookmanager.data.model

import kotlinx.serialization.Serializable
import xyz.kgy_production.webdavebookmanager.util.serializer.LocalDateTimeSerializer
import java.time.LocalDateTime

@Serializable
data class WebDavDirNode(
    val current: String,
    val parent: String? = null,
    val children: List<String> = listOf(),
    @Serializable(with = LocalDateTimeSerializer::class)
    val lastUpdated: LocalDateTime,
)
