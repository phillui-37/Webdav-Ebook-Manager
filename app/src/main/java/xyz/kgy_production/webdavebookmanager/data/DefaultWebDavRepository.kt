package xyz.kgy_production.webdavebookmanager.data

import android.util.Log
import arrow.core.Option
import arrow.core.getOrElse
import arrow.core.none
import arrow.core.raise.option
import arrow.core.toOption
import kotlinx.coroutines.CoroutineDispatcher
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
            dbDAO.getByUrl(url)
                .firstOrNull {
                    val _loginId = it.loginId.decrypt()
                        .onNone {
                            Log.e(
                                "WebDavRepo::getEntryByUrlAndLoginId",
                                "Login id decrypt error"
                            )
                        }
                        .getOrElse { "" }
                    _loginId == loginId
                }
                ?.toModel()
                .toOption()
        }
    }

    override suspend fun createEntry(
        name: Option<String>,
        url: String,
        loginId: Option<String>,
        password: Option<String>,
        byPassPattern: List<WebDavModel.ByPassPattern>,
    ) {
        withContext(dispatcher) {
            var uuid = UUID.randomUUID().toString()
            while (getEntryByUuid(uuid).isSome())
                uuid = UUID.randomUUID().toString()
            option {
                val _loginId = loginId.flatMap(String::encrypt).bind()
                val _pwd = password.flatMap(String::encrypt).bind()
                val webDavEntity = WebDavEntity(
                    name = name.getOrElse { "" },
                    url = url,
                    loginId = _loginId,
                    password = _pwd,
                    isActive = true,
                    uuid = uuid,
                    bypassPattern = Json.encodeToString(byPassPattern)
                )
                dbDAO.insert(webDavEntity)
            }.onNone {
                Log.e("WebDavRepo::createEntry", "Encrypt fail")
            }
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
        byPassPattern: List<WebDavModel.ByPassPattern>,
    ) {
        withContext(dispatcher) {
            option {
                val _loginId = loginId.flatMap(String::decrypt).bind()
                val _pwd = password.flatMap(String::decrypt).bind()
                val entity = WebDavEntity(
                    id = id,
                    name = name.getOrElse { "" },
                    url = url,
                    loginId = _loginId,
                    password = _pwd,
                    isActive = isActive,
                    uuid = uuid,
                    bypassPattern = Json.encodeToString(byPassPattern)
                )
                dbDAO.upsert(entity)
            }.onNone {
                Log.e("WebDavRepo::createEntry", "Decrypt fail")
            }
        }
    }

    override suspend fun updateEntry(model: WebDavModel) {
        withContext(dispatcher) {
            option {
                getEntryById(model.id).bind()
                dbDAO.upsert(model.toEntity())
            }
        }.onNone { Log.w("WebDavRepo::updateEntry", "id not exists, op has stopped") }
    }

    override suspend fun deactivateEntry(id: Int) {
        withContext(dispatcher) {
            option {
                getEntryById(id).bind()
                dbDAO.setIsActive(id, false)
            }
        }.onNone { Log.w("WebDavRepo::deactivateEntry", "id not exists, op has stopped") }
    }

    override suspend fun activateEntry(id: Int) {
        withContext(dispatcher) {
            option {
                getEntryById(id).bind()
                dbDAO.setIsActive(id, true)
            }
        }.onNone { Log.w("WebDavRepo::activateEntry", "id not exists, op has stopped") }
    }

    override suspend fun deleteEntry(id: Int) {
        withContext(dispatcher) {
            option {
                getEntryById(id).bind()
                dbDAO.deleteById(id)
            }
        }.onNone { Log.w("WebDavRepo::deleteEntry", "id not exists, op has stopped") }
    }

}