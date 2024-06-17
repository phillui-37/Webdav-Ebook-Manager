package xyz.kgy_production.webdavebookmanager.viewmodel

import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.flowOf
import xyz.kgy_production.webdavebookmanager.util.ThemeOption
import javax.inject.Inject

@HiltViewModel
class ThemeViewModel @Inject constructor():ViewModel() {
    val themeMode = MutableLiveData<ThemeOption>()
    val isDark = MutableLiveData<Boolean>()
}