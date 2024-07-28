package xyz.kgy_production.webdavebookmanager.data

import android.util.Log
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import xyz.kgy_production.webdavebookmanager.data.localdb.WebDavDAO
import xyz.kgy_production.webdavebookmanager.data.localdb.WebDavEntity
import xyz.kgy_production.webdavebookmanager.data.model.WebDavModel
import xyz.kgy_production.webdavebookmanager.di.DefaultDispatcher
import xyz.kgy_production.webdavebookmanager.util.decrypt
import xyz.kgy_production.webdavebookmanager.util.encrypt
import java.util.UUID
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

    override suspend fun getEntryByUuid(uuid: String): WebDavModel? {
        return withContext(dispatcher) {
            dbDAO.getByUuid(uuid)?.toModel()
        }
    }

    override suspend fun getEntryByUrlAndLoginId(
        url: String,
        loginId: String
    ): WebDavModel? {
        return withContext(dispatcher) {
            dbDAO.getByUrl(url)
                .firstOrNull {
                    val _loginId = it.loginId.decrypt() ?: run {
                        Log.e(
                            "WebDavRepo::getEntryByUrlAndLoginId",
                            "Login id decrypt error"
                        )
                        ""
                    }
                    _loginId == loginId
                }
                ?.toModel()
        }
    }

    override suspend fun createEntry(
        name: String?,
        url: String,
        loginId: String?,
        password: String?,
        byPassPattern: List<WebDavModel.ByPassPattern>,
        defaultOpenByThis: Boolean,
    ) {
        var uuid = UUID.randomUUID().toString()
        while (getEntryByUuid(uuid) != null)
            uuid = UUID.randomUUID().toString()
        loginId?.let(String::encrypt)?.let { _loginId ->
            password?.let(String::encrypt)?.let { _pwd ->
                val webDavEntity = WebDavEntity(
                    name = name ?: "",
                    url = url,
                    loginId = _loginId,
                    password = _pwd,
                    isActive = true,
                    uuid = uuid,
                    bypassPattern = Json.encodeToString(byPassPattern),
                    openByThis = defaultOpenByThis,
                )
                withContext(dispatcher) {
                    dbDAO.insert(webDavEntity)
                }
            } ?: Log.e("WebDavRepo::createEntry", "Encrypt fail")
        } ?: Log.e("WebDavRepo::createEntry", "Encrypt fail")
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
        isActive: Boolean,
        uuid: String,
        byPassPattern: List<WebDavModel.ByPassPattern>,
        defaultOpenByThis: Boolean,
    ) {
        loginId?.let(String::decrypt)?.let { _loginId ->
            password?.let(String::encrypt)?.let { _pwd ->
                val entity = WebDavEntity(
                    id = id,
                    name = name ?: "",
                    url = url,
                    loginId = _loginId,
                    password = _pwd,
                    isActive = isActive,
                    uuid = uuid,
                    bypassPattern = Json.encodeToString(byPassPattern),
                    openByThis = defaultOpenByThis
                )
                withContext(dispatcher) {
                    dbDAO.upsert(entity)
                }
            } ?: Log.e("WebDavRepo::createEntry", "Decrypt fail")
        } ?: Log.e("WebDavRepo::createEntry", "Decrypt fail")
    }

    override suspend fun updateEntry(model: WebDavModel) {
        getEntryById(model.id)?.let {
            withContext(dispatcher) {
                dbDAO.upsert(model.toEntity())
            }
        } ?: Log.w("WebDavRepo::updateEntry", "id not exists, op has stopped")
    }

    override suspend fun deactivateEntry(id: Int) {
        getEntryById(id)?.let {
            withContext(dispatcher) {
                dbDAO.setIsActive(id, false)
            }
        } ?: Log.w("WebDavRepo::deactivateEntry", "id not exists, op has stopped")
    }

    override suspend fun activateEntry(id: Int) {
        getEntryById(id)?.let {
            withContext(dispatcher) {
                dbDAO.setIsActive(id, true)
            }
        } ?: Log.w("WebDavRepo::activateEntry", "id not exists, op has stopped")
    }

    override suspend fun deleteEntry(id: Int) {
        getEntryById(id)?.let {
            withContext(dispatcher) {
                dbDAO.deleteById(id)
            }
        } ?: Log.w("WebDavRepo::deleteEntry", "id not exists, op has stopped")
    }
}