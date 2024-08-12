package xyz.kgy_production.webdavebookmanager.data.repository.impl

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import xyz.kgy_production.webdavebookmanager.data.localdb.dao.DirTreeCacheDAO
import xyz.kgy_production.webdavebookmanager.data.model.DirTreeCacheModel
import xyz.kgy_production.webdavebookmanager.data.model.WebDavCacheData
import xyz.kgy_production.webdavebookmanager.data.repository.DirTreeCacheRepository
import xyz.kgy_production.webdavebookmanager.data.toEntity
import xyz.kgy_production.webdavebookmanager.data.toModel
import xyz.kgy_production.webdavebookmanager.di.DefaultDispatcher
import xyz.kgy_production.webdavebookmanager.util.Logger
import java.time.LocalDateTime
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DefaultDirTreeCacheRepository @Inject constructor(
    private val dbDAO: DirTreeCacheDAO,
    @DefaultDispatcher private val dispatcher: CoroutineDispatcher
) : DirTreeCacheRepository {
    private val logger by Logger.delegate(this::class.java)

    override suspend fun getById(id: Int): DirTreeCacheModel? {
        return withContext(dispatcher) {
            dbDAO.getById(id)?.toModel()
        }
    }

    override suspend fun create(data: DirTreeCacheModel) {
        withContext(dispatcher) {
            dbDAO.insert(data.toEntity())
        }
    }

    override suspend fun update(data: DirTreeCacheModel) {
        withContext(dispatcher) {
            getById(data.id)?.let {
                dbDAO.upsert(data.copy(lastUpdated = LocalDateTime.now()).toEntity())
            } ?: logger.w("[update] id not exists")
        }
    }

    override suspend fun updateContent(id: Int, content: WebDavCacheData) {
        withContext(dispatcher) {
            getById(id)?.let {
                dbDAO.upsert(
                    it.copy(webDavCacheData = content, lastUpdated = LocalDateTime.now()).toEntity()
                )
            } ?: logger.w("[updateContent] id not exists")
        }
    }

    override suspend fun deleteById(id: Int) {
        withContext(dispatcher) {
            getById(id)?.let {
                dbDAO.deleteById(id)
            } ?: logger.w("[deleteById] id not exists")
        }
    }
}