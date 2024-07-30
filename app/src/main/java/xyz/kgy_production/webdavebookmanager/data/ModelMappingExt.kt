package xyz.kgy_production.webdavebookmanager.data

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import xyz.kgy_production.webdavebookmanager.data.localdb.entity.WebDavEntity
import xyz.kgy_production.webdavebookmanager.data.model.WebDavModel
import xyz.kgy_production.webdavebookmanager.util.Logger
import xyz.kgy_production.webdavebookmanager.util.decrypt
import xyz.kgy_production.webdavebookmanager.util.encrypt

fun WebDavEntity.toModel(): WebDavModel {
    val logger by Logger.delegate(this::class.java)
    return WebDavModel(
        id = id,
        name = name,
        url = url,
        loginId = loginId.decrypt() ?: run {
            logger.e("Entity->Model: Decrypt fail")
            ""
        },
        password = password.decrypt() ?: run {
            logger.e("Entity->Model: Decrypt fail")
            ""
        },
        isActive = isActive,
        uuid = uuid,
        bypassPattern = Json.decodeFromString(bypassPattern),
        defaultOpenByThis = openByThis
    )
}

fun WebDavModel.toEntity(): WebDavEntity {
    val logger by Logger.delegate(this::class.java)
    return WebDavEntity(
        id = id,
        name = name,
        url = url,
        loginId = loginId.encrypt() ?: run {
            logger.e("Model->Entity: Encrypt fail")
            ""
        },
        password = password.encrypt() ?: run {
            logger.e("Model->Entity: Encrypt fail")
            ""
        },
        isActive = isActive,
        uuid = uuid,
        bypassPattern = Json.encodeToString(bypassPattern),
        openByThis = defaultOpenByThis
    )
}
