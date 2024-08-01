package xyz.kgy_production.webdavebookmanager.ui.component

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.stringResource
import xyz.kgy_production.webdavebookmanager.R

@Composable
fun FilterView(
    onFilter: () -> Unit, //TODO
) {
    var expanded by remember { mutableStateOf(false) }

    // TODO
    if (expanded)
        TextButton(onClick = { expanded = false }) {
            Text(text = "under construction")
        }
    else
        IconButton(onClick = { expanded = true }) {
            Icon(Icons.Filled.FilterList, stringResource(id = R.string.label_common_filter))
        }
}