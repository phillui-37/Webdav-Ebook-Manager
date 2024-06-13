package xyz.kgy_production.webdavebookmanager.data

import xyz.kgy_production.webdavebookmanager.data.localdb.WebDavEntity
import xyz.kgy_production.webdavebookmanager.data.model.WebDavModel
import xyz.kgy_production.webdavebookmanager.util.decrypt
import xyz.kgy_production.webdavebookmanager.util.encrypt

fun WebDavEntity.toModel() = WebDavModel(
    id = id,
    name = name,
    url = url,
    loginId = loginId.decrypt(),
    password = password.decrypt(),
    isActive = isActive
)

fun WebDavModel.toEntity() = WebDavEntity(
    id = id,
    name = name,
    url = url,
    loginId = loginId.encrypt(),
    password = password.encrypt(),
    isActive = isActive
)