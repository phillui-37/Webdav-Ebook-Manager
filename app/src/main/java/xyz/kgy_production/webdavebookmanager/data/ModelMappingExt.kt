package xyz.kgy_production.webdavebookmanager.data

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import xyz.kgy_production.webdavebookmanager.data.localdb.entity.DirTreeCacheEntity
import xyz.kgy_production.webdavebookmanager.data.localdb.entity.WebDavEntity
import xyz.kgy_production.webdavebookmanager.data.model.DirTreeCacheModel
import xyz.kgy_production.webdavebookmanager.data.model.WebDavCacheData
import xyz.kgy_production.webdavebookmanager.data.model.WebDavModel
import xyz.kgy_production.webdavebookmanager.util.Logger
import xyz.kgy_production.webdavebookmanager.util.decrypt
import xyz.kgy_production.webdavebookmanager.util.encrypt
import xyz.kgy_production.webdavebookmanager.util.formatDateTime
import java.time.format.DateTimeFormatter

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

fun DirTreeCacheEntity.toModel(): DirTreeCacheModel {
    val data = Json.decodeFromString<WebDavCacheData>(jsonContent)
    return DirTreeCacheModel(
        id,
        webDavId,
        data,
        lastUpdated.formatDateTime(DateTimeFormatter.ISO_DATE_TIME)
    )
}

fun DirTreeCacheModel.toEntity(): DirTreeCacheEntity {
    val dataStr = Json.encodeToString(webDavCacheData)
    return DirTreeCacheEntity(
        id,
        webDavId,
        dataStr,
        lastUpdated.format(DateTimeFormatter.ISO_DATE_TIME)
    )
}