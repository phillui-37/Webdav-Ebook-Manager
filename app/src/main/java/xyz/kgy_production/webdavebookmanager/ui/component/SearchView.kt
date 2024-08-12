package xyz.kgy_production.webdavebookmanager.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import xyz.kgy_production.webdavebookmanager.LocalIsDarkTheme
import xyz.kgy_production.webdavebookmanager.util.Logger

@Composable
fun SearchView(
    initialSearchContent: String = "",
    onSearch: (String) -> Unit,
    onCollapse: () -> Unit,
) {
    val logger by Logger.delegate("SearchView")
    val isDarkTheme = LocalIsDarkTheme.current
    var searchContent by remember { mutableStateOf(initialSearchContent) }

    fun onSearchTextChanged(newValue: String) {
        searchContent = newValue
        CoroutineScope(Dispatchers.IO).launch {
            val oldText = searchContent
            delay(300L)
            if (oldText == searchContent)
                onSearch(searchContent)
        }.start()
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 5.dp)
            .background(if (isDarkTheme) Color.Black else Color.White)
    ) {
        Icon(Icons.Filled.Search, "Search", Modifier.align(Alignment.CenterVertically)) // TODO i18n
        TextField(
            modifier = Modifier
                .weight(1f)
                .border(1.dp, if (isDarkTheme) Color.White else Color.Black, CircleShape),
            value = searchContent,
            onValueChange = ::onSearchTextChanged,
            singleLine = true,
            colors = TextFieldDefaults.colors(
                focusedContainerColor = Color.Transparent,
                unfocusedContainerColor = Color.Transparent,
            )
        )
        IconButton(modifier = Modifier.align(Alignment.CenterVertically), onClick = {
            onSearchTextChanged("")
            onCollapse()
        }) {
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, "Close search dialog") // TODO i18n
        }
    }
}

@Composable
fun SearchIconButton(onExpand: () -> Unit) {
    IconButton(onClick = onExpand) {
        Icon(Icons.Filled.Search, "Search") // TODO i18n
    }
}

@Preview
@Composable
fun SearchViewPreview() {
    SearchView(onSearch = {}) {}
}
