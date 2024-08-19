package xyz.kgy_production.webdavebookmanager.ui.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.serialization.json.Json
import xyz.kgy_production.webdavebookmanager.MainActivity
import xyz.kgy_production.webdavebookmanager.data.model.WebDavCacheData
import xyz.kgy_production.webdavebookmanager.data.model.WebDavModel
import xyz.kgy_production.webdavebookmanager.data.repository.WebDavRepository
import xyz.kgy_production.webdavebookmanager.util.BOOK_METADATA_CONFIG_FILENAME
import xyz.kgy_production.webdavebookmanager.util.Logger
import xyz.kgy_production.webdavebookmanager.util.getFileFromWebDav
import xyz.kgy_production.webdavebookmanager.util.getWebDavDirContentList
import xyz.kgy_production.webdavebookmanager.util.removeWebDavCache
import xyz.kgy_production.webdavebookmanager.util.saveWebDavCache
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val webDavRepository: WebDavRepository,
) : ViewModel() {
    private val logger by Logger.delegate(this::class.java)

    private var webdavDomainList: List<WebDavModel> = listOf()
    private val _filteredWebDavDomainListFlow = MutableStateFlow<List<WebDavModel>>(listOf())
    val filteredWebdavDomainListFlow: StateFlow<List<WebDavModel>>
        get() = _filteredWebDavDomainListFlow

    suspend fun fetchWebDavDomainList() {
        webdavDomainList = webDavRepository.getAllEntries()
        _filteredWebDavDomainListFlow.emit(webdavDomainList)
    }

    suspend fun removeEntry(id: Int, ctx: Context) {
        if (id < 0) {
            logger.w("[removeEntry] ID < 0, in dev mode?")
            return
        }
        try {
            val uuid = webDavRepository.getEntryById(id)!!.uuid
            ctx.removeWebDavCache(uuid)
            webDavRepository.deleteEntry(id)
            webdavDomainList = webdavDomainList.filter { it.id != id }
            _filteredWebDavDomainListFlow.emit(listOf())
            delay(500) // workaround to ensure the network availability icon display correctly
            _filteredWebDavDomainListFlow.emit(webdavDomainList)
        } catch (e: Exception) {
            logger.e(e.message ?: "Error occurred")
            e.printStackTrace()
        }
    }

    suspend fun filterWebdavList(filterText: String) {
        _filteredWebDavDomainListFlow.emit(
            webdavDomainList.filter {
                it.name.contains(filterText) || it.url.contains(filterText)
            }
        )
    }

    suspend fun downloadRemoteCache(ctx: Context, model: WebDavModel) {
        logger.d("[downloadRemoteCache] start")
        MainActivity.keepScreenOn()
        val pendingList = mutableListOf<String>()
        val doneList = mutableListOf<String>()

        pendingList.add(model.url)
        while (pendingList.isNotEmpty()) {
            val url = pendingList.removeFirst()
            if (doneList.contains(url)) continue
            logger.d("handling $url")
            val dirLs = mutableListOf<DirectoryViewModel.ContentData>()
            getWebDavDirContentList(
                url,
                model.loginId,
                model.password
            ) {
                dirLs.addAll(it)
            }
            dirLs.filter { it.isDir }
                .also {
                    logger.d("add ${it.size} to pending")
                    pendingList.addAll(it.filter { !doneList.contains(it.fullUrl) }
                        .map { it.fullUrl })
                }
            try {
                val cache = getFileFromWebDav(
                    "$url/$BOOK_METADATA_CONFIG_FILENAME",
                    model.loginId,
                    model.password
                )?.let {
                    Json.decodeFromString<WebDavCacheData>(it.decodeToString())
                }
                logger.d("got cache data of $url, isNull: ${cache == null}")
                if (cache != null)
                    ctx.saveWebDavCache(
                        cache,
                        model.uuid,
                        url.replace(model.url, "").removePrefix("/")
                    )
                doneList.add(url)
                logger.d("finished ${doneList.size}")
            } catch (e: Exception) {
                logger.e("[downloadRemoteCache] error", e)
                pendingList.add(url)
            }
        }

        MainActivity.letScreenRest()
        logger.d("[downloadRemoteCache] done")
    }
}