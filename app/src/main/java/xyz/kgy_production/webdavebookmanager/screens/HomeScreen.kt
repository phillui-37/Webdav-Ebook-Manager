package xyz.kgy_production.webdavebookmanager.screens

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.TextUnitType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import arrow.core.Option
import arrow.core.Some
import arrow.core.none
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import xyz.kgy_production.webdavebookmanager.R
import xyz.kgy_production.webdavebookmanager.component.HomeTopBar
import xyz.kgy_production.webdavebookmanager.data.model.WebDavModel
import xyz.kgy_production.webdavebookmanager.ui.theme.INTERNAL_HORIZONTAL_PADDING_MODIFIER
import xyz.kgy_production.webdavebookmanager.ui.theme.INTERNAL_VERTICAL_PADDING_MODIFIER
import xyz.kgy_production.webdavebookmanager.ui.theme.WebdavEbookManagerTheme
import xyz.kgy_production.webdavebookmanager.util.checkAvailability
import xyz.kgy_production.webdavebookmanager.util.matchParentHeight
import xyz.kgy_production.webdavebookmanager.util.matchParentWidth
import xyz.kgy_production.webdavebookmanager.viewmodel.HomeViewModel
import java.net.URL
import java.util.UUID

@Composable
fun HomeScreen(
    openDrawer: () -> Unit,
    toEditWebDavScreen: (Option<String>) -> Unit,
    toDirectoryScreen: (Option<String>) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: HomeViewModel = hiltViewModel()
) {
    val coroutineScope = CoroutineScope(Dispatchers.IO)
    var entryToDelete by remember { mutableStateOf<Option<Pair<Int, String>>>(none()) }
    var refreshCbList by remember { mutableStateOf<List<suspend () -> Unit>>(listOf()) }

    if (entryToDelete.isSome()) {
        val entry = entryToDelete.getOrNull()!!
        DeleteEntryDialog(
            url = entry.second,
            onCancel = { },
            onDelete = {
                coroutineScope.launch {
                    viewModel.removeEntry(entry.first)
                }
            },
            finalCb = { entryToDelete = none() }
        )
    }

    Scaffold(
        topBar = {
            TopBar(
                openDrawer = openDrawer,
                onFilterSites = { viewModel.filterWebdavList(it) },
                onRefresh = {
                    refreshCbList.forEach {
                        coroutineScope.launch { it() }
                    }
                }
            )
        },
        floatingActionButton = {
            Fab { toEditWebDavScreen(none()) }
        }
    ) { padding ->
        Column(
            modifier = modifier
                .padding(padding)
                .then(INTERNAL_HORIZONTAL_PADDING_MODIFIER)
                .then(INTERNAL_VERTICAL_PADDING_MODIFIER)
        ) {
            LazyColumn(
                contentPadding = PaddingValues(vertical = 10.dp)
            ) {
                items(viewModel.filteredWebdavDomainListLiveData.value) { model ->
                    WebDavCard(
                        model = model,
                        coroutineScope = coroutineScope,
                        onClick = { toEditWebDavScreen(Some(it)) },
                        toShowDeleteDialog = { entryToDelete = Some(model.id to model.url) },
                        refreshCb = { refreshCbList += it }
                    )
                }
            }
        }
    }
}

@Composable
private fun DeleteEntryDialog(
    url: String,
    onDelete: () -> Unit,
    onCancel: () -> Unit,
    finalCb: () -> Unit,
) {
    AlertDialog(
        title = { Text(text = stringResource(id = R.string.dialog_webdav_delete_title)) },
        text = { Text(text = stringResource(id = R.string.dialog_webdav_delete_text).format(url)) },
        dismissButton = {
            TextButton(onClick = {
                onCancel()
                finalCb()
            }) {
                Text(text = stringResource(id = R.string.btn_cancel))
            }
        },
        onDismissRequest = {
            onCancel()
            finalCb()
        },
        confirmButton = {
            TextButton(onClick = {
                onDelete()
                finalCb()
            }) {
                Text(text = stringResource(id = R.string.btn_confirm))
            }
        })
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun WebDavCard(
    model: WebDavModel,
    coroutineScope: CoroutineScope,
    onClick: (String) -> Unit,
    toShowDeleteDialog: () -> Unit,
    refreshCb: (suspend () -> Unit) -> Unit,
) {
    var isAvailable by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        coroutineScope.launch {
            isAvailable = URL(model.url).checkAvailability()
        }
    }

    refreshCb {
        isAvailable = URL(model.url).checkAvailability()
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .wrapContentHeight()
            .shadow(5.dp)
            .combinedClickable(
                onClick = { onClick(model.uuid) },
                onLongClick = toShowDeleteDialog,
                onLongClickLabel = stringResource(id = R.string.btn_delete_webdav_desc)
            ),
    ) {
        Row {
            Column(
                modifier = Modifier.weight(.85f)
            ) {
                Text(text = model.name.ifEmpty { "-" }, fontSize = TextUnit(20f, TextUnitType.Sp))
                Box(modifier = Modifier.height(5.dp))
                Text(text = "URL: ${model.url}")
                Text(text = "Login ID: ${model.loginId}")
            }
            Box(
                modifier = Modifier.weight(.15f).align(Alignment.CenterVertically),
            ) {
                Column(
                    modifier = Modifier.matchParentHeight(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    Box(
                        modifier = Modifier
                            .size(20.dp)
                            .clip(CircleShape)
                            .background(if (isAvailable) Color.Green else Color.Red)
                    )
                    Text(
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth(),
                        text = stringResource(id = if (isAvailable) R.string.status_online else R.string.status_offline)
                    )
                }
            }
        }
    }
}

@Composable
private fun TopBar(
    openDrawer: () -> Unit,
    onFilterSites: (String) -> Unit,
    onRefresh: () -> Unit,
) {
    HomeTopBar(
        title = stringResource(id = R.string.screen_home_title),
        openDrawer = openDrawer,
        onFilterSites = onFilterSites,
        onRefresh = onRefresh
    )
}

@Composable
private fun Fab(
    onClick: () -> Unit,
) {
    FloatingActionButton(
        onClick = onClick,
    ) {
        Icon(
            Icons.Filled.Add,
            stringResource(id = R.string.btn_add_webdav_desc)
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
    WebdavEbookManagerTheme {
        Greeting("Android")
    }
}