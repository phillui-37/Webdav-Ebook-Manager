package xyz.kgy_production.webdavebookmanager.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import xyz.kgy_production.webdavebookmanager.data.localdb.WebDavEntity

class HomeViewModel: ViewModel() {
    private val webdavDomainList: MutableList<WebDavEntity> = mutableListOf()
    private val _filteredWebDavDomainListLiveData = MutableLiveData<List<WebDavEntity>>(listOf())
    val filteredWebdavDomainListLiveData: LiveData<List<WebDavEntity>>
        get() = _filteredWebDavDomainListLiveData

    init {
        // todo load webdav domain list from sqlite? shared preference?
    }

    /** todo data from webdav model */
    fun filterWebdavList(filterText: String) {
        _filteredWebDavDomainListLiveData.postValue(
            webdavDomainList.filter {
                it.name.contains(filterText) || it.url.contains(filterText)
            }
        )
    }
}