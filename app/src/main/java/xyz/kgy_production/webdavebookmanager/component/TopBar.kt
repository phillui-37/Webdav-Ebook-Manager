package xyz.kgy_production.webdavebookmanager.component

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TopBar(
    openDrawer:() -> Unit,
    onFilterSites: (String) -> Unit,
    onRefresh: () -> Unit,
) {
    TopAppBar(
        title = { Text("TopBar") }, /** todo **/
        navigationIcon = {
            IconButton(onClick = openDrawer) {
                Icon(Icons.Filled.Menu, "Open Drawer") /**todo**/
            }
        },
        modifier = Modifier.fillMaxWidth(),
        actions = {
            HomeTaskMenu(onFilterSites = onFilterSites)
        }
    )
}

@Composable
fun HomeTaskMenu(
    onFilterSites: (String) -> Unit
) {

    SearchView(onSearch = onFilterSites)
}

@Preview
@Composable
private fun HomeTaskMenuPreview() {
    HomeTaskMenu {}
}