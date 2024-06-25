package xyz.kgy_production.webdavebookmanager.viewmodel

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.runBlocking
import xyz.kgy_production.webdavebookmanager.data.WebDavRepository
import xyz.kgy_production.webdavebookmanager.data.localdb.WebDavEntity
import xyz.kgy_production.webdavebookmanager.data.model.WebDavModel
import javax.inject.Inject
import kotlin.coroutines.CoroutineContext

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val webDavRepository: WebDavRepository,
): ViewModel() {
    private var webdavDomainList: List<WebDavModel> = listOf()
    private val _filteredWebDavDomainListLiveData = MutableStateFlow<List<WebDavModel>>(listOf())
    val filteredWebdavDomainListLiveData: StateFlow<List<WebDavModel>>
        get() = _filteredWebDavDomainListLiveData


    init {
        runBlocking(Dispatchers.IO) {
            webdavDomainList = webDavRepository.getAllEntries()
            _filteredWebDavDomainListLiveData.value = webdavDomainList
        }
    }

    suspend fun removeEntry(id: Int) {
        if (id < 0) {
            Log.w("HomeViewModel::removeEntry", "ID < 0, in dev mode?")
            return
        }
        try {
            webDavRepository.deleteEntry(id)
            webdavDomainList = webdavDomainList.filter { it.id != id }
            _filteredWebDavDomainListLiveData.value = webdavDomainList
        } catch (e: Exception) {
            Log.e("HomeViewModel::removeEntry", e.message ?: "Error occurred")
            e.printStackTrace()
        }
    }

    /** todo data from webdav model */
    fun filterWebdavList(filterText: String) {
        _filteredWebDavDomainListLiveData.value =
            webdavDomainList.filter {
                it.name.contains(filterText) || it.url.contains(filterText)
            }
    }
}