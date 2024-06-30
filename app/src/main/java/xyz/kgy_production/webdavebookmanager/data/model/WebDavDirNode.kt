package xyz.kgy_production.webdavebookmanager.data.model

import kotlinx.serialization.Serializable

@Serializable
data class WebDavDirNode(
    val current: String,
    val parent: String? = null,
    val children: List<String> = listOf(),
)
