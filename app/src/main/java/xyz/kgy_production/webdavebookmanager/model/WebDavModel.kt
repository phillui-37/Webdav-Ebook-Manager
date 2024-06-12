package xyz.kgy_production.webdavebookmanager.model

data class WebDavModel(
    val name: String = "",
    val url: String,
    val loginId: String = "",
    val password: String = "",
    /* todo webdav server specific setting? */
)