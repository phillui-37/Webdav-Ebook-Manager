package xyz.kgy_production.webdavebookmanager.ui.viewmodel

import android.content.Context
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import xyz.kgy_production.webdavebookmanager.MainApplication.Companion.dataStore
import xyz.kgy_production.webdavebookmanager.util.ConfigKey
import xyz.kgy_production.webdavebookmanager.util.ThemeOption
import xyz.kgy_production.webdavebookmanager.util.isSystemDarkMode
import javax.inject.Inject
import kotlin.coroutines.CoroutineContext

@HiltViewModel
class ThemeViewModel @Inject constructor() : ViewModel() {
    val themeMode = MutableStateFlow(ThemeOption.AUTO)
    val isDark = MutableStateFlow(false)

    suspend fun updateThemeSetting(opt: ThemeOption, dataStore: DataStore<Preferences>) {
        themeMode.value = opt
        dataStore.edit { pref ->
            pref[stringPreferencesKey(ConfigKey.THEME_OPTION.name)] = opt.name
        }
    }

    fun getThemeSetting(dataStore: DataStore<Preferences>): String? {
        return runBlocking(Dispatchers.IO) {
            dataStore.data
                .map { it[stringPreferencesKey(ConfigKey.THEME_OPTION.name)] }
                .firstOrNull()
        }
    }

    @Composable
    fun getIsDarkState(dataStore: DataStore<Preferences>): State<Boolean> {
        return isDark.collectAsState(
            initial = getThemeSetting(dataStore)?.let {
                it == ThemeOption.DARK.name || (it == ThemeOption.AUTO.name && isSystemInDarkTheme())
            } ?: isSystemInDarkTheme()
        )
    }

    suspend fun subscribeThemeModeChange(ctx: Context) {
        themeMode.collect {
            isDark.value =
                if (it == ThemeOption.AUTO) ctx.isSystemDarkMode() else it == ThemeOption.DARK
        }
    }
}

typealias FnUpdateThemeSetting = suspend (ThemeOption) -> Unit