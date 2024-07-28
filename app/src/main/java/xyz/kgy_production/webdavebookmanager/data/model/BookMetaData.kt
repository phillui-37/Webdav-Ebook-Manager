package xyz.kgy_production.webdavebookmanager.data.model

import kotlinx.serialization.Serializable
import xyz.kgy_production.webdavebookmanager.util.serializer.LocalDateTimeSerializer
import java.time.LocalDateTime

@Serializable
data class BookMetaData(
    val name: String,
    val series: String = NOT_FILLED,
    val orderInSeries: Int = -1,
    private val _tag: String = "", // separated by ,
    val author: String = NOT_FILLED,
    val publisher: String = NOT_FILLED,
    val fileType: String, // from MediaType
    val relativePath: String,
    @Serializable(with = LocalDateTimeSerializer::class)
    val lastUpdated: LocalDateTime,
) {
    companion object {
        const val NOT_FILLED = "NOT FILLED"
        const val NOT_AVAILABLE = "N/A"
    }

    val tag: List<String>
        get() = _tag.split(",")
}
