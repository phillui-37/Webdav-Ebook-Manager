package xyz.kgy_production.webdavebookmanager.component

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import xyz.kgy_production.webdavebookmanager.R


/** Home */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeTopBar(
    title: String,
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
        modifier = Modifier.fillMaxWidth(),
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
    TopAppBar(
        title = { Text(title) },
        navigationIcon = {
            IconButton(onClick = openDrawer) {
                Icon(Icons.Filled.Menu, stringResource(id = R.string.btn_drawer_desc))
            }
        },
        modifier = Modifier.fillMaxWidth()
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
