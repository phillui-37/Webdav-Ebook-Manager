package xyz.kgy_production.webdavebookmanager.ui.viewmodel

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import xyz.kgy_production.webdavebookmanager.data.repository.WebDavRepository
import javax.inject.Inject

@HiltViewModel
class EbookViewModel @Inject constructor(
    private val webDavRepository: WebDavRepository
) : ViewModel() {
    suspend fun getWebDavModel(id: Int) = webDavRepository.getEntryById(id)
}