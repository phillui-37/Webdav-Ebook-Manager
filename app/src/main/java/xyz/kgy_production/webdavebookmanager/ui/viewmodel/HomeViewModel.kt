package xyz.kgy_production.webdavebookmanager.ui.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import xyz.kgy_production.webdavebookmanager.data.model.WebDavModel
import xyz.kgy_production.webdavebookmanager.data.repository.WebDavRepository
import xyz.kgy_production.webdavebookmanager.util.Logger
import xyz.kgy_production.webdavebookmanager.util.removeWebDavCache
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val webDavRepository: WebDavRepository,
): ViewModel() {
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
}