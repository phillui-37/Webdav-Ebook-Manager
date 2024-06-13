package xyz.kgy_production.webdavebookmanager.data

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.withContext
import xyz.kgy_production.webdavebookmanager.data.localdb.WebDavDAO
import xyz.kgy_production.webdavebookmanager.data.localdb.WebDavEntity
import xyz.kgy_production.webdavebookmanager.data.model.WebDavModel
import xyz.kgy_production.webdavebookmanager.di.ApplicationScope
import xyz.kgy_production.webdavebookmanager.di.DefaultDispatcher
import xyz.kgy_production.webdavebookmanager.util.encrypt
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DefaultWebDavRepository @Inject constructor(
    private val dbDAO: WebDavDAO,
    @DefaultDispatcher private val dispatcher: CoroutineDispatcher,
) : WebDavRepository {
    override suspend fun getAllEntries(): List<WebDavModel> {
        return withContext(dispatcher) {
            dbDAO.getAll()
                .map { it.toModel() }
        }
    }

    override suspend fun getEntryById(id: Int): WebDavModel? {
        return withContext(dispatcher) {
            dbDAO.getById(id)?.toModel()
        }
    }

    override suspend fun createEntry(
        name: String?,
        url: String,
        loginId: String?,
        password: String?
    ) {
        withContext(dispatcher) {
            dbDAO.insert(
                WebDavEntity(
                    name = name ?: "",
                    url = url,
                    loginId = loginId?.encrypt() ?: "",
                    password = password?.encrypt() ?: "",
                    isActive = true
                )
            )
        }
    }

    override suspend fun createEntry(model: WebDavModel) {
        withContext(dispatcher) {
            dbDAO.insert(model.toEntity())
        }
    }

    override suspend fun updateEntry(
        id: Int,
        name: String?,
        url: String,
        loginId: String?,
        password: String?,
        isActive: Boolean
    ) {
        withContext(dispatcher) {
            dbDAO.upsert(
                WebDavEntity(
                    id = id,
                    name = name ?: "",
                    url = url,
                    loginId = loginId?.encrypt() ?: "",
                    password = password?.encrypt() ?: "",
                    isActive = isActive
                )
            )
        }
    }

    override suspend fun updateEntry(model: WebDavModel) {
        withContext(dispatcher) {
            getEntryById(model.id) ?: return@withContext
            dbDAO.upsert(model.toEntity())
        }
    }

    override suspend fun deactivateEntry(id: Int) {
        withContext(dispatcher) {
            getEntryById(id) ?: return@withContext
            dbDAO.setIsActive(id, false)
        }
    }

    override suspend fun activateEntry(id: Int) {
        withContext(dispatcher) {
            getEntryById(id) ?: return@withContext
            dbDAO.setIsActive(id, false)
        }
    }

    override suspend fun deleteEntry(id: Int) {
        withContext(dispatcher) {
            getEntryById(id) ?: return@withContext
            dbDAO.deleteById(id)
        }
    }

}