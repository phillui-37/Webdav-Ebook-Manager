package xyz.kgy_production.webdavebookmanager.screens

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.ViewModel
import xyz.kgy_production.webdavebookmanager.R
import xyz.kgy_production.webdavebookmanager.component.HomeTopBar
import xyz.kgy_production.webdavebookmanager.ui.theme.WebdavEbookManagerTheme
import xyz.kgy_production.webdavebookmanager.viewmodel.HomeViewModel

@Composable
fun HomeScreen(
    openDrawer: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ViewModel = HomeViewModel()
) {
    Scaffold(
        topBar = {
            HomeTopBar(
                title = stringResource(id = R.string.screen_home_title),
                openDrawer = openDrawer,
                onFilterSites = {/*TODO*/},
                onRefresh = {/*TODO*/}
            )
        }
    ) { padding ->
        Greeting(
            name = "Fuck you",
            modifier = modifier.padding(padding)
        )
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