package xyz.kgy_production.webdavebookmanager.data.model

import java.time.LocalDateTime

data class DirTreeCacheModel(
    val id: Int,
    val webDavId: Int,
    val webDavCacheData: WebDavCacheData,
    val lastUpdated: LocalDateTime
) {

}
