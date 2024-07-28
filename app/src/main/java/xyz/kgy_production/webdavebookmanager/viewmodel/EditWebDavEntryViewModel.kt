package xyz.kgy_production.webdavebookmanager.viewmodel

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import xyz.kgy_production.webdavebookmanager.common.Result
import xyz.kgy_production.webdavebookmanager.data.WebDavRepository
import xyz.kgy_production.webdavebookmanager.data.model.WebDavModel
import javax.inject.Inject

@HiltViewModel
class EditWebDavEntryViewModel @Inject constructor(
    private val webDavRepository: WebDavRepository
) : ViewModel() {
    private var originalData = WebDavModel(url = "")
    private val _data = MutableStateFlow(WebDavModel(url=""))
    val data: StateFlow<WebDavModel>
        get() = _data

    suspend fun setModelByUuid(uuid: String): Result<Unit> {
        val entry = webDavRepository.getEntryByUuid(uuid)
        return entry?.let {
            originalData = it
            _data.value = it
            Result.ok(Unit)
        } ?: Result.fail("UUID not valid")
    }

    fun updateModel(model: WebDavModel) {
        _data.value = model
    }

    fun resetModel() {
        _data.value = originalData
    }

    suspend fun addWebDavEntry(): String? {
        val err = _data.value.validate()
        if (err == null)
            webDavRepository.createEntry(_data.value)
        return err
    }

    suspend fun editWebEntry(): String? {
        val err = _data.value.validate()
        if (err == null)
            webDavRepository.updateEntry(_data.value)
        return err
    }
}