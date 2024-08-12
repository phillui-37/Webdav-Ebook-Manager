package xyz.kgy_production.webdavebookmanager.data.repository

import xyz.kgy_production.webdavebookmanager.data.model.DirTreeCacheModel
import xyz.kgy_production.webdavebookmanager.data.model.WebDavCacheData

interface DirTreeCacheRepository {
    suspend fun getById(id: Int): DirTreeCacheModel?
    suspend fun create(data: DirTreeCacheModel)
    suspend fun update(data: DirTreeCacheModel)
    suspend fun updateContent(id: Int, content: WebDavCacheData)
    suspend fun deleteById(id: Int)
}