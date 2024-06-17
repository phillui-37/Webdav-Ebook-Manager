package xyz.kgy_production.webdavebookmanager

import android.content.res.Configuration
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Modifier
import androidx.datastore.preferences.core.stringPreferencesKey
import arrow.core.valid
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.runBlocking
import xyz.kgy_production.webdavebookmanager.MainApplication.Companion.dataStore
import xyz.kgy_production.webdavebookmanager.ui.theme.WebdavEbookManagerTheme
import xyz.kgy_production.webdavebookmanager.util.ConfigKey
import xyz.kgy_production.webdavebookmanager.util.ThemeOption
import xyz.kgy_production.webdavebookmanager.util.eq
import xyz.kgy_production.webdavebookmanager.util.isSystemDarkMode
import xyz.kgy_production.webdavebookmanager.viewmodel.ThemeViewModel

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    private val themeViewModel by viewModels<ThemeViewModel>()
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        var themeSetting: String?

        runBlocking(Dispatchers.IO) {
            themeSetting = dataStore.data
                .map { it[stringPreferencesKey(ConfigKey.THEME_OPTION.name)] }
                .firstOrNull()
        }

        setContent {
            val isDarkState = themeViewModel.isDark.observeAsState(
                initial = themeSetting?.let {
                    it == ThemeOption.DARK.name || (it == ThemeOption.AUTO.name && isSystemInDarkTheme())
                } ?: isSystemInDarkTheme()
            )
            themeViewModel.isDark.observe(this) {
                Log.d("isDark", it.toString())
            }
            themeViewModel.themeMode.observe(this) {
                Log.d("themeMode", it.name)
                themeViewModel.isDark.value = if (it == ThemeOption.AUTO) isSystemDarkMode() else it == ThemeOption.DARK
            }
            WebdavEbookManagerTheme(
                isDarkTheme = isDarkState.value
            ) {
                NaviGraph(themeViewModel = themeViewModel, modifier = Modifier.padding())
            }
        }
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        if (themeViewModel.themeMode.value == ThemeOption.AUTO) {
            themeViewModel.isDark.value = isSystemDarkMode()
        }
    }
}

