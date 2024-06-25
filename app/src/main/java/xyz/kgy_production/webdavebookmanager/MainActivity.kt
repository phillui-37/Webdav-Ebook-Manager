package xyz.kgy_production.webdavebookmanager

import android.content.res.Configuration
import android.net.ConnectivityManager
import android.net.Network
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.core.view.WindowCompat
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import arrow.core.valid
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import xyz.kgy_production.webdavebookmanager.MainApplication.Companion.dataStore
import xyz.kgy_production.webdavebookmanager.ui.theme.WebdavEbookManagerTheme
import xyz.kgy_production.webdavebookmanager.util.ConfigKey
import xyz.kgy_production.webdavebookmanager.util.ThemeOption
import xyz.kgy_production.webdavebookmanager.util.eq
import xyz.kgy_production.webdavebookmanager.util.isSystemDarkMode
import xyz.kgy_production.webdavebookmanager.viewmodel.ThemeViewModel

val LocalIsDarkTheme = staticCompositionLocalOf { false }
val LocalIsNetworkAvailable = staticCompositionLocalOf { true }

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    private val themeViewModel by viewModels<ThemeViewModel>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            val isDarkState = themeViewModel.getIsDarkState(dataStore = dataStore)
            var isNetworkAvailable by remember { mutableStateOf(true) }

            LaunchedEffect(Unit) {
                CoroutineScope(Dispatchers.IO).launch {
                    themeViewModel.subscribeThemeModeChange(this@MainActivity)
                }
                CoroutineScope(Dispatchers.Main).launch {
                    subscribeNetworkChange { isNetworkAvailable = it }
                }
            }
            CompositionLocalProvider(
                LocalIsDarkTheme provides isDarkState.value,
                LocalIsNetworkAvailable provides isNetworkAvailable,
            ) {
                WebdavEbookManagerTheme {
                    NaviGraph(
                        modifier = Modifier.padding(),
                        updateThemeSetting = { opt ->
                            themeViewModel.updateThemeSetting(opt, dataStore)
                        }
                    )
                }
            }
        }
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        if (themeViewModel.themeMode.value == ThemeOption.AUTO) {
            themeViewModel.isDark.value = isSystemDarkMode()
        }
    }

    private fun subscribeNetworkChange(setter: (Boolean) -> Unit) {
        Log.d("MainAct::subscribeNetworkChange", "start")
        val cm = getSystemService(ConnectivityManager::class.java)
        cm.registerDefaultNetworkCallback(object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                Log.d("MainAct::subscribeNetworkChange", "Network available")
                setter(true)
            }
            override fun onLost(network: Network) {
                Log.d("MainAct::subscribeNetworkChange", "Network unavailable")
                setter(false)
            }
        })
    }
}

