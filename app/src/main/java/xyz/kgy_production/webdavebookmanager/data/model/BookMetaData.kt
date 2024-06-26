package xyz.kgy_production.webdavebookmanager.data.model

import kotlinx.serialization.Serializable

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
) {
    companion object {
        const val NOT_FILLED = "NOT FILLED"
        const val NOT_AVAILABLE = "N/A"
    }

    val tag: List<String>
        get() = _tag.split(",")
}
