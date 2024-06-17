package xyz.kgy_production.webdavebookmanager.screens

import android.util.Log
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Button
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.lifecycle.ViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import xyz.kgy_production.webdavebookmanager.MainApplication.Companion.dataStore
import xyz.kgy_production.webdavebookmanager.R
import xyz.kgy_production.webdavebookmanager.component.SettingTopBar
import xyz.kgy_production.webdavebookmanager.util.ConfigKey
import xyz.kgy_production.webdavebookmanager.util.ThemeOption
import xyz.kgy_production.webdavebookmanager.util.eq
import xyz.kgy_production.webdavebookmanager.viewmodel.SettingViewModel
import xyz.kgy_production.webdavebookmanager.viewmodel.ThemeViewModel

suspend fun DataStore<Preferences>.updateThemeOption(
    themeViewModel: ThemeViewModel,
    opt: ThemeOption
) {
    edit {
        it[stringPreferencesKey(ConfigKey.THEME_OPTION.name)] = opt.name
    }
    themeViewModel.themeMode.value = opt
}


@Composable
fun SettingScreen(
    openDrawer: () -> Unit,
    themeViewModel: ThemeViewModel,
    coroutineScope: CoroutineScope,
    modifier: Modifier = Modifier,
    viewModel: ViewModel = SettingViewModel()
) {
    val dataStore = LocalContext.current.dataStore

    Scaffold(
        topBar = {
            SettingTopBar(
                title = stringResource(id = R.string.screen_setting_title),
                openDrawer = openDrawer
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = modifier
                .fillMaxWidth()
                .padding(padding)
                .wrapContentHeight()
        ) {
            item {
                Row(
                    modifier = Modifier.fillParentMaxWidth()
                ) {
                    Button(
                        modifier = Modifier.fillParentMaxWidth(1f / 3),
                        onClick = {
                            coroutineScope.launch {
                                dataStore.updateThemeOption(themeViewModel, ThemeOption.AUTO)
                            }
                        }
                    ) {
                        Text(text = stringResource(id = R.string.theme_mode_auto))
                    }
                    Button(
                        modifier = Modifier.fillParentMaxWidth(1f / 3),
                        onClick = {
                            coroutineScope.launch {
                                dataStore.updateThemeOption(themeViewModel, ThemeOption.DARK)
                            }
                        }
                    ) {
                        Text(text = stringResource(id = R.string.theme_mode_dark))
                    }
                    Button(
                        modifier = Modifier.fillParentMaxWidth(1f / 3),
                        onClick = {
                            coroutineScope.launch {
                                dataStore.updateThemeOption(themeViewModel, ThemeOption.LIGHT)
                            }
                        }
                    ) {
                        Text(text = stringResource(id = R.string.theme_mode_light))
                    }
                }
            }
        }
    }
}