package xyz.kgy_production.webdavebookmanager.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import arrow.core.Option
import arrow.core.Some

@Composable
fun EditWebDavEntryScreen(
    uuid: Option<String>,
    onBack: () -> Unit,
) {
    Scaffold { padding ->
        Column(
            modifier = Modifier.padding(padding)
        ) {
            Button(onClick = onBack) {
                Text(text = "Back!")
            }
        }
    }
}

@Preview
@Composable
private fun EditWebDavEntryScreenPreview() {
    EditWebDavEntryScreen(uuid = Some("test")) {

    }
}