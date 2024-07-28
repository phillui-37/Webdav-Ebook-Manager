package xyz.kgy_production.webdavebookmanager.data

import android.util.Log
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import xyz.kgy_production.webdavebookmanager.data.localdb.WebDavEntity
import xyz.kgy_production.webdavebookmanager.data.model.WebDavModel
import xyz.kgy_production.webdavebookmanager.util.decrypt
import xyz.kgy_production.webdavebookmanager.util.encrypt

fun WebDavEntity.toModel() = WebDavModel(
    id = id,
    name = name,
    url = url,
    loginId = loginId.decrypt()?: run {
        Log.e("Webdav:Entity->Model", "Decrypt fail")
        ""
    },
    password = password.decrypt()?: run {
        Log.e("Webdav:Entity->Model", "Decrypt fail")
        ""
    },
    isActive = isActive,
    uuid = uuid,
    bypassPattern = Json.decodeFromString(bypassPattern)
)

fun WebDavModel.toEntity() = WebDavEntity(
    id = id,
    name = name,
    url = url,
    loginId = loginId.encrypt() ?: run {
        Log.e("Webdav:Model->Entity", "Encrypt fail")
        ""
    },
    password = password.encrypt() ?: run {
        Log.e("Webdav:Model->Entity", "Encrypt fail")
        ""
    },
    isActive = isActive,
    uuid = uuid,
    bypassPattern = Json.encodeToString(bypassPattern)
)