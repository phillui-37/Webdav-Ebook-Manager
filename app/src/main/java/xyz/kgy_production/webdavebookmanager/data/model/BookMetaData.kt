package xyz.kgy_production.webdavebookmanager.data.model

import kotlinx.serialization.Serializable

@Serializable
data class BookMetaData(
    val name: String,
    val series: String,
    val orderInSeries: Int,
    val tag: String, // separated by ,
    val author: String,
    val fileType: String, // from MediaType
    val relativePath: String,
)
