package xyz.kgy_production.webdavebookmanager.data

import xyz.kgy_production.webdavebookmanager.data.model.WebDavModel

interface WebDavRepository {
    suspend fun getAllEntries(): List<WebDavModel>
    suspend fun getEntryById(id: Int): WebDavModel?
    suspend fun createEntry(name: String?, url: String, loginId: String?, password: String?)
    suspend fun createEntry(model: WebDavModel)
    suspend fun updateEntry(id: Int, name: String?, url: String, loginId: String?, password: String?, isActive: Boolean)
    suspend fun updateEntry(model: WebDavModel)
    suspend fun deactivateEntry(id: Int)
    suspend fun activateEntry(id: Int)
    suspend fun deleteEntry(id: Int)
}