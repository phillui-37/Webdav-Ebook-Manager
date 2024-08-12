package xyz.kgy_production.webdavebookmanager.data.repository

import xyz.kgy_production.webdavebookmanager.data.model.WebDavModel

interface WebDavRepository {
    suspend fun getAllEntries(): List<WebDavModel>
    suspend fun getEntryById(id: Int): WebDavModel?
    suspend fun getEntryByUuid(uuid: String): WebDavModel?
    suspend fun getEntryByUrlAndLoginId(url: String, loginId: String): WebDavModel?
    suspend fun createEntry(
        name: String?,
        url: String,
        loginId: String?,
        password: String?,
        byPassPattern: List<WebDavModel.ByPassPattern>,
        defaultOpenByThis: Boolean,
    )

    suspend fun createEntry(model: WebDavModel)
    suspend fun updateEntry(
        id: Int,
        name: String?,
        url: String,
        loginId: String?,
        password: String?,
        isActive: Boolean,
        uuid: String,
        byPassPattern: List<WebDavModel.ByPassPattern>,
        defaultOpenByThis: Boolean,
    )

    suspend fun updateEntry(model: WebDavModel)
    suspend fun deactivateEntry(id: Int)
    suspend fun activateEntry(id: Int)
    suspend fun deleteEntry(id: Int)
}