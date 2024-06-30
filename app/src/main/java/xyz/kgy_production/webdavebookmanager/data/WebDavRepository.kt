package xyz.kgy_production.webdavebookmanager.data

import arrow.core.Option
import xyz.kgy_production.webdavebookmanager.data.model.WebDavModel

interface WebDavRepository {
    suspend fun getAllEntries(): List<WebDavModel>
    suspend fun getEntryById(id: Int): Option<WebDavModel>
    suspend fun getEntryByUuid(uuid: String): Option<WebDavModel>
    suspend fun getEntryByUrlAndLoginId(url: String, loginId: String): Option<WebDavModel>
    suspend fun createEntry(
        name: Option<String>,
        url: String,
        loginId: Option<String>,
        password: Option<String>,
        byPassPattern: List<WebDavModel.ByPassPattern>,
    )

    suspend fun createEntry(model: WebDavModel)
    suspend fun updateEntry(
        id: Int,
        name: Option<String>,
        url: String,
        loginId: Option<String>,
        password: Option<String>,
        isActive: Boolean,
        uuid: String,
        byPassPattern: List<WebDavModel.ByPassPattern>,
    )

    suspend fun updateEntry(model: WebDavModel)
    suspend fun deactivateEntry(id: Int)
    suspend fun activateEntry(id: Int)
    suspend fun deleteEntry(id: Int)
}