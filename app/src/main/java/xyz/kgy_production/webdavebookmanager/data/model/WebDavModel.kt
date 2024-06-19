package xyz.kgy_production.webdavebookmanager.data.model

import xyz.kgy_production.webdavebookmanager.data.localdb.WebDavEntity

data class WebDavModel(
    val id: Int = 0,
    val uuid: String = "",
    val name: String = "",
    val url: String,
    val loginId: String = "",
    val password: String = "",
    val isActive: Boolean = true
)
