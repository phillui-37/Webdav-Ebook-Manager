package xyz.kgy_production.webdavebookmanager.component

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Shapes
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun SearchView(
    modifier: Modifier = Modifier,
    initialSearchContent: String = "",
    onSearch: (String) -> Unit
) {
    var searchContent by remember { mutableStateOf(initialSearchContent) }
    var isExpanded by remember { mutableStateOf(false) }

    fun onSearchTextChanged(newValue: String) {
        searchContent = newValue
        CoroutineScope(Dispatchers.IO).launch {
            val oldText = searchContent
            delay(300L)
            if (oldText == searchContent)
                onSearch(searchContent)
        }.start()
    }

    if (isExpanded) {
        Row(
            modifier = modifier.fillMaxWidth()
                .border(1.dp, Color.Black, CircleShape)
        ) {
            Icon(Icons.Filled.Search, "Search")
            TextField(value = searchContent, onValueChange = ::onSearchTextChanged)
            IconButton(onClick = { isExpanded = false }) {
                Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, "Close search dialog")
            }
        }
    } else {
        IconButton(modifier = modifier, onClick = { isExpanded = true}) {
            Icon(Icons.Filled.Search, "Search")
        }
    }
}

@Preview
@Composable
fun SearchViewPreview() {
    SearchView {}
}