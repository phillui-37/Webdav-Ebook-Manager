package xyz.kgy_production.webdavebookmanager.screens

import androidx.compose.foundation.layout.Column
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
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.ViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import xyz.kgy_production.webdavebookmanager.R
import xyz.kgy_production.webdavebookmanager.component.SettingTopBar
import xyz.kgy_production.webdavebookmanager.ui.theme.INTERNAL_HORIZONTAL_PADDING_MODIFIER
import xyz.kgy_production.webdavebookmanager.ui.theme.INTERNAL_VERTICAL_PADDING_MODIFIER
import xyz.kgy_production.webdavebookmanager.util.ThemeOption
import xyz.kgy_production.webdavebookmanager.viewmodel.FnUpdateThemeSetting
import xyz.kgy_production.webdavebookmanager.viewmodel.SettingViewModel


@Composable
fun SettingScreen(
    isDarkTheme: Boolean,
    openDrawer: () -> Unit,
    updateThemeSetting: FnUpdateThemeSetting,
    coroutineScope: CoroutineScope,
    modifier: Modifier = Modifier,
    viewModel: ViewModel = SettingViewModel()
) {
    Scaffold(
        topBar = {
            SettingTopBar(
                title = stringResource(id = R.string.screen_setting_title),
                isDarkTheme = isDarkTheme,
                openDrawer = openDrawer
            )
        },
    ) { padding ->
        LazyColumn(
            modifier = modifier
                .fillMaxWidth()
                .padding(padding)
                .wrapContentHeight()
                .then(INTERNAL_VERTICAL_PADDING_MODIFIER)
                .then(INTERNAL_HORIZONTAL_PADDING_MODIFIER)
        ) {
            item {
                Column(modifier = Modifier.fillParentMaxWidth()) {
                    Text(text = stringResource(id = R.string.setting_theme_setting_title))
                    Row(modifier = Modifier.fillParentMaxWidth()) {
                        Button(
                            modifier = Modifier.fillParentMaxWidth(1f / 3),
                            onClick = {
                                coroutineScope.launch {
                                    updateThemeSetting(ThemeOption.AUTO)
                                }
                            }
                        ) {
                            Text(text = stringResource(id = R.string.theme_mode_auto))
                        }
                        Button(
                            modifier = Modifier.fillParentMaxWidth(1f / 3),
                            onClick = {
                                coroutineScope.launch {
                                    updateThemeSetting(ThemeOption.DARK)
                                }
                            }
                        ) {
                            Text(text = stringResource(id = R.string.theme_mode_dark))
                        }
                        Button(
                            modifier = Modifier.fillParentMaxWidth(1f / 3),
                            onClick = {
                                coroutineScope.launch {
                                    updateThemeSetting(ThemeOption.LIGHT)
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
}