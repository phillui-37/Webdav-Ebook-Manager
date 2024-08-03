package xyz.kgy_production.webdavebookmanager

import android.Manifest
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.net.ConnectivityManager
import android.net.Network
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import xyz.kgy_production.webdavebookmanager.MainApplication.Companion.dataStore
import xyz.kgy_production.webdavebookmanager.data.WebDavRepository
import xyz.kgy_production.webdavebookmanager.ui.component.GenericEbookView
import xyz.kgy_production.webdavebookmanager.ui.theme.WebdavEbookManagerTheme
import xyz.kgy_production.webdavebookmanager.ui.viewmodel.ThemeViewModel
import xyz.kgy_production.webdavebookmanager.util.Logger
import xyz.kgy_production.webdavebookmanager.util.ThemeOption
import xyz.kgy_production.webdavebookmanager.util.isSystemDarkMode
import java.io.File
import javax.inject.Inject

val LocalIsDarkTheme = staticCompositionLocalOf { false }
val LocalIsNetworkAvailable = staticCompositionLocalOf { true }
val LocalPrivateStorage: ProvidableCompositionLocal<File?> = staticCompositionLocalOf { null }

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    @Inject
    lateinit var webDavRepository: WebDavRepository

    companion object {
        private var _instance: ComponentActivity? = null
        private val logger by Logger.delegate(MainActivity::class.java)

        fun keepScreenOn() {
            logger.d("Requested Keep Screen On")
            CoroutineScope(Dispatchers.Main).launch {
                _instance?.window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            }
        }

        fun letScreenRest() {
            logger.d("Cancelled Keep Screen On")
            CoroutineScope(Dispatchers.Main).launch {
                _instance?.window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            }
        }
    }

    private val themeViewModel by viewModels<ThemeViewModel>()

    private val singlePermissionRegister = registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
        // TODO
    }

    private fun requireRequiredPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val isGranted = ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
            if (!isGranted)
                singlePermissionRegister.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        _instance = this

        enableEdgeToEdge()

        setContent {
            val isDarkState = themeViewModel.getIsDarkState(dataStore = dataStore)
            var isNetworkAvailable by remember { mutableStateOf(true) }
            val model = runBlocking { webDavRepository.getEntryById(1) }

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
                LocalPrivateStorage provides getExternalFilesDir(null),
            ) {
                WebdavEbookManagerTheme {
                    NaviGraph(
                        modifier = Modifier.padding(),
                        updateThemeSetting = { opt ->
                            themeViewModel.updateThemeSetting(opt, dataStore)
                        }
                    )
                }
                // to load js/css asap
                if (isNetworkAvailable && model != null) {
                    val uri = Uri.parse("android.resource://${packageName}/${R.raw.epub}")
                    GenericEbookView(
                        modifier = Modifier.size(1.dp),
                        webDavId = 1,
                        fileUri = uri,
                    ) { logger.e(it) }
                }
            }
        }
        requireRequiredPermission()
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        if (themeViewModel.themeMode.value == ThemeOption.AUTO) {
            themeViewModel.isDark.value = isSystemDarkMode()
        }
    }

    private fun subscribeNetworkChange(
        setter: (Boolean) -> Unit
    ) {
        logger.d("[subscribeNetworkChange] start")
        val cm = getSystemService(ConnectivityManager::class.java)
        cm.registerDefaultNetworkCallback(object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                logger.d("[subscribeNetworkChange] Network available")
                setter(true)
            }

            override fun onLost(network: Network) {
                logger.d("[subscribeNetworkChange] Network unavailable")
                setter(false)
            }
        })
    }
}

