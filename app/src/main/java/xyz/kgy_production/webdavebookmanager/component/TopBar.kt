package xyz.kgy_production.webdavebookmanager.component

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBackIosNew
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import arrow.core.Option
import arrow.core.none
import arrow.core.raise.option
import xyz.kgy_production.webdavebookmanager.LocalIsDarkTheme
import xyz.kgy_production.webdavebookmanager.R
import xyz.kgy_production.webdavebookmanager.ui.theme.TOP_BAR_DARK_MODIFIER
import xyz.kgy_production.webdavebookmanager.ui.theme.TOP_BAR_WHITE_MODIFIER

/** Home */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeTopBar(
    title: String,
    openDrawer: () -> Unit,
    onFilterSites: (String) -> Unit,
    onRefresh: () -> Unit,
) {
    val isDarkTheme = LocalIsDarkTheme.current
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
    openDrawer: () -> Unit
) {
    val isDarkTheme = LocalIsDarkTheme.current
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

/** Other **/
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CommonTopBar(
    title: String,
    onBack: () -> Unit,
    onSearch: Option<(String) -> Unit> = none()
) {
    val isDarkTheme = LocalIsDarkTheme.current
    TopAppBar(
        title = { Text(text = title) },
        navigationIcon = {
            IconButton(onClick = onBack) {
                Icon(Icons.Filled.ArrowBackIosNew, stringResource(id = R.string.btn_back_desc))
            }
        },
        modifier = if (isDarkTheme) TOP_BAR_DARK_MODIFIER else TOP_BAR_WHITE_MODIFIER,
        actions = {
            onSearch.onSome { SearchView(onSearch = it) }
        }
    )
}

/** for non root */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DirectoryTopBar(
    title: String,
    onBack: () -> Unit,
    toParentDir: () -> Unit,
    onSearch: (String) -> Unit,
    onFilter: () -> Unit, // TODO
) {
    val isDarkTheme = LocalIsDarkTheme.current
    TopAppBar(
        title = { Text(text = title) },
        navigationIcon = {
            IconButton(onClick = onBack) {
                Icon(Icons.Filled.ArrowBackIosNew, stringResource(id = R.string.btn_back_desc))
            }
        },
        modifier = if (isDarkTheme) TOP_BAR_DARK_MODIFIER else TOP_BAR_WHITE_MODIFIER,
        actions = {
            SearchView(onSearch = onSearch)
            FilterView(onFilter = onFilter) // TODO
            IconButton(onClick = toParentDir) {
                Icon(Icons.Filled.ArrowUpward, stringResource(id = R.string.label_to_parent_dir))
            }
        }
    )
}

/** Preview */
@Preview
@Composable
private fun HomeTopBarPreview() {
    HomeTopBar(
        title = stringResource(id = R.string.screen_home_title),
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
        openDrawer = {}
    )
}
