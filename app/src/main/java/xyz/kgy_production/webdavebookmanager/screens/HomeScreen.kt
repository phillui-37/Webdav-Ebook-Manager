package xyz.kgy_production.webdavebookmanager.screens

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.ViewModel
import arrow.core.Option
import arrow.core.Some
import xyz.kgy_production.webdavebookmanager.R
import xyz.kgy_production.webdavebookmanager.component.HomeTopBar
import xyz.kgy_production.webdavebookmanager.ui.theme.INTERNAL_HORIZONTAL_PADDING_MODIFIER
import xyz.kgy_production.webdavebookmanager.ui.theme.INTERNAL_VERTICAL_PADDING_MODIFIER
import xyz.kgy_production.webdavebookmanager.ui.theme.WebdavEbookManagerTheme
import xyz.kgy_production.webdavebookmanager.viewmodel.HomeViewModel

@Composable
fun HomeScreen(
    isDarkTheme: Boolean,
    openDrawer: () -> Unit,
    toEditWebDavScreen: (Option<String>) -> Unit,
    toDirectoryScreen: (Option<String>) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ViewModel = HomeViewModel()
) {
    Scaffold(
        topBar = {
            HomeTopBar(
                title = stringResource(id = R.string.screen_home_title),
                isDarkTheme = isDarkTheme,
                openDrawer = openDrawer,
                onFilterSites = {/*TODO*/},
                onRefresh = {/*TODO*/}
            )
        },
    ) { padding ->
        Column(
            modifier = modifier.padding(padding)
                .then(INTERNAL_HORIZONTAL_PADDING_MODIFIER)
                .then(INTERNAL_VERTICAL_PADDING_MODIFIER)
        ) {
            Button(onClick = { toEditWebDavScreen(Some("uuid")) }) {
                Text(text = "To Edit")
            }
            Greeting(
                name = "Hi",
                modifier = modifier
            )
        }
    }
}

@Composable
private fun Greeting(name: String, modifier: Modifier = Modifier) {
    Text(
        text = "Hello wow $name!",
        modifier = modifier
    )
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    WebdavEbookManagerTheme(isSystemInDarkTheme()) {
        Greeting("Android")
    }
}