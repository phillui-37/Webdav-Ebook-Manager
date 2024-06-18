package xyz.kgy_production.webdavebookmanager.viewmodel

import android.content.Context
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.runBlocking
import xyz.kgy_production.webdavebookmanager.MainApplication.Companion.dataStore
import xyz.kgy_production.webdavebookmanager.util.ConfigKey
import xyz.kgy_production.webdavebookmanager.util.ThemeOption
import xyz.kgy_production.webdavebookmanager.util.isSystemDarkMode
import javax.inject.Inject

@HiltViewModel
class ThemeViewModel @Inject constructor():ViewModel() {
    val themeMode = MutableLiveData<ThemeOption>()
    val isDark = MutableLiveData<Boolean>()

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
        return isDark.observeAsState(
            initial = getThemeSetting(dataStore)?.let {
                it == ThemeOption.DARK.name || (it == ThemeOption.AUTO.name && isSystemInDarkTheme())
            } ?: isSystemInDarkTheme()
        )
    }

    fun subscribeThemeModeChange(lifecycleOwner: LifecycleOwner, ctx: Context) {
        themeMode.observe(lifecycleOwner) {
            isDark.value =
                if (it == ThemeOption.AUTO) ctx.isSystemDarkMode() else it == ThemeOption.DARK
        }
    }
}

typealias FnUpdateThemeSetting = suspend (ThemeOption) -> Unit