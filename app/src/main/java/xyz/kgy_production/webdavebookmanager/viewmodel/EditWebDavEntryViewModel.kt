package xyz.kgy_production.webdavebookmanager.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import arrow.core.Either
import arrow.core.Option
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import xyz.kgy_production.webdavebookmanager.data.WebDavRepository
import xyz.kgy_production.webdavebookmanager.data.model.WebDavModel
import xyz.kgy_production.webdavebookmanager.data.toEntity
import javax.inject.Inject

@HiltViewModel
class EditWebDavEntryViewModel @Inject constructor(
    private val webDavRepository: WebDavRepository
) : ViewModel() {
    private var originalData = WebDavModel(url = "")
    private val _data = MutableStateFlow(WebDavModel(url=""))
    val data: StateFlow<WebDavModel>
        get() = _data

    suspend fun setModelByUuid(uuid: String): Either<String, Unit> {
        val entry = webDavRepository.getEntryByUuid(uuid)
        return if (entry.isSome()) {
            originalData = entry.getOrNull()!!
            _data.value = originalData
            Either.Right(Unit)
        } else {
            Either.Left("UUID not valid")
        }
    }

    fun updateModel(model: WebDavModel) {
        _data.value = model
    }

    fun resetModel() {
        _data.value = originalData
    }

    suspend fun addWebDavEntry(): Option<String> {
        val err = _data.value.validate()
        if (err.isNone())
            webDavRepository.createEntry(_data.value)
        return err
    }

    suspend fun editWebEntry(): Option<String> {
        val err = _data.value.validate()
        if (err.isNone())
            webDavRepository.updateEntry(_data.value)
        return err
    }
}