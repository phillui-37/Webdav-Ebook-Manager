package xyz.kgy_production.webdavebookmanager.viewmodel

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.runBlocking
import xyz.kgy_production.webdavebookmanager.data.WebDavRepository
import xyz.kgy_production.webdavebookmanager.data.model.WebDavModel
import xyz.kgy_production.webdavebookmanager.util.Logger
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



    init {
        runBlocking(Dispatchers.IO) {
            webdavDomainList = webDavRepository.getAllEntries()
            _filteredWebDavDomainListFlow.value = webdavDomainList
        }
    }

    suspend fun removeEntry(id: Int) {
        if (id < 0) {
            logger.w("[removeEntry] ID < 0, in dev mode?")
            return
        }
        try {
            webDavRepository.deleteEntry(id)
            webdavDomainList = webdavDomainList.filter { it.id != id }
            _filteredWebDavDomainListFlow.value = webdavDomainList
        } catch (e: Exception) {
            logger.e(e.message ?: "Error occurred")
            e.printStackTrace()
        }
    }

    /** todo data from webdav model */
    fun filterWebdavList(filterText: String) {
        _filteredWebDavDomainListFlow.value =
            webdavDomainList.filter {
                it.name.contains(filterText) || it.url.contains(filterText)
            }
    }
}