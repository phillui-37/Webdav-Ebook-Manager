package xyz.kgy_production.webdavebookmanager.screens

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import xyz.kgy_production.webdavebookmanager.ui.theme.WebdavEbookManagerTheme

@Composable
fun HomeScreen(modifier: Modifier) {
    Greeting(
        name = "Fuck you",
        modifier = modifier
    )
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
    WebdavEbookManagerTheme {
        Greeting("Android")
    }
}