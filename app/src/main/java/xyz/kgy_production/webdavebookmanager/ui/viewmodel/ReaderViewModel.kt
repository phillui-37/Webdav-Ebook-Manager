package xyz.kgy_production.webdavebookmanager.ui.viewmodel

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import xyz.kgy_production.webdavebookmanager.data.WebDavRepository
import javax.inject.Inject

@HiltViewModel
class ReaderViewModel @Inject constructor(
    private val webDavRepository: WebDavRepository
) : ViewModel() {
    suspend fun getWebDavModel(webDavId: Int) = webDavRepository.getEntryById(webDavId)
}