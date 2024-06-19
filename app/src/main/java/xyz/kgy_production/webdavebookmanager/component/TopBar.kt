package xyz.kgy_production.webdavebookmanager.component

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import xyz.kgy_production.webdavebookmanager.R
import xyz.kgy_production.webdavebookmanager.ui.theme.TOP_BAR_DARK_MODIFIER
import xyz.kgy_production.webdavebookmanager.ui.theme.TOP_BAR_WHITE_MODIFIER

/** Home */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeTopBar(
    title: String,
    isDarkTheme: Boolean,
    openDrawer: () -> Unit,
    onFilterSites: (String) -> Unit,
    onRefresh: () -> Unit,
) {
    TopAppBar(
        title = { Text(title) },
        navigationIcon = {
            IconButton(onClick = openDrawer) {
                Icon(Icons.Filled.Menu, stringResource(id = R.string.btn_drawer_desc))
            }
        },
        modifier = if (isDarkTheme) TOP_BAR_DARK_MODIFIER else TOP_BAR_WHITE_MODIFIER,
        actions = {
            HomeTaskMenu(onFilterSites = onFilterSites)
            RefreshButton(onRefresh = onRefresh)
        }
    )
}

@Composable
fun HomeTaskMenu(onFilterSites: (String) -> Unit) {
    SearchView(onSearch = onFilterSites)
}

@Composable
fun RefreshButton(onRefresh: () -> Unit) {
    IconButton(onClick = onRefresh) {
        Icon(Icons.Filled.Refresh, stringResource(id = R.string.std_refresh_content))
    }
}

/** Setting */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingTopBar(
    title: String,
    isDarkTheme: Boolean,
    openDrawer: () -> Unit
) {
    TopAppBar(
        title = { Text(title) },
        navigationIcon = {
            IconButton(onClick = openDrawer) {
                Icon(Icons.Filled.Menu, stringResource(id = R.string.btn_drawer_desc))
            }
        },
        modifier = if (isDarkTheme) TOP_BAR_DARK_MODIFIER else TOP_BAR_WHITE_MODIFIER
    )
}

/** Preview */
@Preview
@Composable
private fun HomeTopBarPreview() {
    HomeTopBar(
        title = stringResource(id = R.string.screen_home_title),
        isDarkTheme = isSystemInDarkTheme(),
        openDrawer = {},
        onFilterSites = {},
        onRefresh = {}
    )
}

@Preview
@Composable
private fun SettingTopBarPreview() {
    SettingTopBar(
        title = stringResource(id = R.string.screen_setting_title),
        isDarkTheme = isSystemInDarkTheme(),
        openDrawer = {}
    )
}
