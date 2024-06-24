package xyz.kgy_production.webdavebookmanager.data

import android.util.Log
import arrow.core.Option
import arrow.core.getOrElse
import arrow.core.toOption
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
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

    override suspend fun getEntryById(id: Int): Option<WebDavModel> {
        return withContext(dispatcher) {
            dbDAO.getById(id)?.toModel().toOption()
        }
    }

    override suspend fun getEntryByUuid(uuid: String): Option<WebDavModel> {
        return withContext(dispatcher) {
            dbDAO.getByUuid(uuid)?.toModel().toOption()
        }
    }

    override suspend fun getEntryByUrlAndLoginId(
        url: String,
        loginId: String
    ): Option<WebDavModel> {
        return withContext(dispatcher) {
            dbDAO.getByUrlAndLoginId(url, loginId)?.toModel().toOption()
        }
    }

    override suspend fun createEntry(
        name: Option<String>,
        url: String,
        loginId: Option<String>,
        password: Option<String>,
    ) {
        Log.d(">>>", "$name;$url;$loginId;$password")
        withContext(dispatcher) {
            var uuid = UUID.randomUUID().toString()
            while (getEntryByUuid(uuid).isSome())
                uuid = UUID.randomUUID().toString()
            val _loginId = loginId.flatMap { it.encrypt() }
                .onNone {
                    Log.e("WebDavRepo::createEntry", "Encrypt fail")
                }
                .getOrElse { "" }
            val _pwd = password.flatMap { it.encrypt() }
                .onNone {
                    Log.e("WebDavRepo::createEntry", "Encrypt fail")
                }
                .getOrElse { "" }
            dbDAO.insert(
                WebDavEntity(
                    name = name.getOrElse { "" },
                    url = url,
                    loginId = _loginId,
                    password = _pwd,
                    isActive = true,
                    uuid = uuid
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
        name: Option<String>,
        url: String,
        loginId: Option<String>,
        password: Option<String>,
        isActive: Boolean,
        uuid: String,
    ) {
        withContext(dispatcher) {
            val _loginId = loginId.flatMap { it.decrypt() }
                .onNone {
                    Log.e("WebDavRepo::createEntry", "Decrypt fail")
                }
                .getOrElse { "" }
            val _pwd = password.flatMap { it.decrypt() }
                .onNone {
                    Log.e("WebDavRepo::createEntry", "Decrypt fail")
                }
                .getOrElse { "" }
            dbDAO.upsert(
                WebDavEntity(
                    id = id,
                    name = name.getOrElse { "" },
                    url = url,
                    loginId = _loginId,
                    password = _pwd,
                    isActive = isActive,
                    uuid = uuid
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