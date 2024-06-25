package xyz.kgy_production.webdavebookmanager.screens

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.outlined.Circle
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
import androidx.compose.ui.window.Dialog
import androidx.hilt.navigation.compose.hiltViewModel
import arrow.core.Option
import arrow.core.Some
import arrow.core.none
import arrow.core.raise.option
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import xyz.kgy_production.webdavebookmanager.LocalIsNetworkAvailable
import xyz.kgy_production.webdavebookmanager.R
import xyz.kgy_production.webdavebookmanager.component.HomeTopBar
import xyz.kgy_production.webdavebookmanager.data.model.WebDavModel
import xyz.kgy_production.webdavebookmanager.ui.theme.INTERNAL_HORIZONTAL_PADDING_MODIFIER
import xyz.kgy_production.webdavebookmanager.ui.theme.INTERNAL_VERTICAL_PADDING_MODIFIER
import xyz.kgy_production.webdavebookmanager.util.checkAvailability
import xyz.kgy_production.webdavebookmanager.util.matchParentHeight
import xyz.kgy_production.webdavebookmanager.util.matchParentWidth
import xyz.kgy_production.webdavebookmanager.util.pipe
import xyz.kgy_production.webdavebookmanager.viewmodel.HomeViewModel
import java.net.URL

private data class DeleteEntryData(
    val id: Int,
    val url: String,
    val loginId: String
)

private data class MenuData(
    val onCancel: () -> Unit,
    val onShowDeleteDialog: () -> Unit,
    val toDirectory: () -> Unit,
    val toWebdavDetail: () -> Unit,
    val finalCb: () -> Unit,
)

@Composable
fun HomeScreen(
    openDrawer: () -> Unit,
    toEditWebDavScreen: (Option<String>) -> Unit,
    toDirectoryScreen: (Option<String>) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: HomeViewModel = hiltViewModel()
) {
    val coroutineScope = CoroutineScope(Dispatchers.IO)
    var entryToDelete by remember { mutableStateOf<Option<DeleteEntryData>>(none()) }
    var refreshCbList by remember { mutableStateOf<List<suspend () -> Unit>>(listOf()) }
    var menuData by remember { mutableStateOf<Option<MenuData>>(none()) }

    option {
        val entry = entryToDelete.bind()
        DeleteEntryDialog(
            url = entry.url,
            loginId = entry.loginId,
            onCancel = { },
            onDelete = {
                coroutineScope.launch {
                    viewModel.removeEntry(entry.id)
                }
            },
            finalCb = { entryToDelete = none() }
        )
    }

    option {
        val data = menuData.bind()
        MenuDialog(
            onCancel = data.onCancel,
            onShowDeleteDialog = data.onShowDeleteDialog,
            toDirectory = data.toDirectory,
            toWebdavDetail = data.toWebdavDetail,
            finalCb = data.finalCb,
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
                        toShowMenuDialog = {
                            menuData = Some(MenuData(
                                onCancel = { },
                                onShowDeleteDialog = {
                                    entryToDelete =
                                        Some(
                                            DeleteEntryData(
                                                model.id,
                                                model.url,
                                                model.loginId
                                            )
                                        )
                                },
                                toDirectory = {
                                    toDirectoryScreen(Some(model.url))
                                },
                                toWebdavDetail = {
                                    toEditWebDavScreen(Some(model.uuid))
                                },
                                finalCb = { menuData = none() }
                            ))
                        },
                        refreshCb = { refreshCbList += it }
                    )
                }
            }
        }
    }
}

@Composable
private fun MenuDialog(
    onCancel: () -> Unit,
    onShowDeleteDialog: () -> Unit,
    toDirectory: () -> Unit,
    toWebdavDetail: () -> Unit,
    finalCb: () -> Unit,
) {
    Dialog(
        onDismissRequest = onCancel pipe finalCb
    ) {
        Card(
            modifier = Modifier
                .wrapContentSize(),
        ) {
            Column(
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.wrapContentSize()
                    .padding(10.dp)
            ) {
                Text(
                    text = stringResource(id = R.string.dialog_webdav_menu_text),
                    textAlign = TextAlign.Center
                )
                TextButton(onClick = toDirectory pipe finalCb, modifier = Modifier.fillMaxWidth()) {
                    Text(text = stringResource(id = R.string.dialog_webdav_menu_option_to_dir))
                }
                TextButton(onClick = toWebdavDetail pipe finalCb, modifier = Modifier.fillMaxWidth()) {
                    Text(text = stringResource(id = R.string.dialog_webdav_menu_option_edit))
                }
                TextButton(onClick = onShowDeleteDialog pipe finalCb, modifier = Modifier.fillMaxWidth()) {
                    Text(text = stringResource(id = R.string.dialog_webdav_menu_option_del))
                }
            }
        }
    }
}

@Composable
private fun DeleteEntryDialog(
    url: String,
    loginId: String,
    onDelete: () -> Unit,
    onCancel: () -> Unit,
    finalCb: () -> Unit,
) {
    AlertDialog(
        title = { Text(text = stringResource(id = R.string.dialog_webdav_delete_title)) },
        text = {
            Text(
                text = stringResource(id = R.string.dialog_webdav_delete_text).format(
                    url,
                    loginId
                )
            )
        },
        dismissButton = {
            TextButton(onClick = onCancel pipe finalCb) {
                Text(text = stringResource(id = R.string.btn_cancel))
            }
        },
        onDismissRequest = onCancel pipe finalCb,
        confirmButton = {
            TextButton(onClick = onDelete pipe finalCb) {
                Text(text = stringResource(id = R.string.btn_confirm))
            }
        })
}

@Composable
private fun WebDavCard(
    model: WebDavModel,
    coroutineScope: CoroutineScope,
    toShowMenuDialog: () -> Unit,
    refreshCb: (suspend () -> Unit) -> Unit,
) {
    val isNetworkAvailable = LocalIsNetworkAvailable.current
    var isAvailable by remember { mutableStateOf(isNetworkAvailable) }
    // todo offline handling: cache dir tree and book

    LaunchedEffect(Unit) {
        coroutineScope.launch {
            isAvailable = isNetworkAvailable && URL(model.url).checkAvailability()
        }
    }

    refreshCb {
        isAvailable = isNetworkAvailable && URL(model.url).checkAvailability()
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 10.dp)
            .wrapContentHeight()
            .shadow(5.dp),
        onClick = toShowMenuDialog,
    ) {
        Row(
            modifier = Modifier.padding(4.dp)
        ) {
            Column(
                modifier = Modifier.weight(.85f)
            ) {
                Text(text = model.name.ifEmpty { "-" }, fontSize = TextUnit(20f, TextUnitType.Sp))
                Box(modifier = Modifier.height(5.dp))
                Text(text = "URL: ${model.url}")
                Text(text = "Login ID: ${model.loginId}")
            }
            Box(
                modifier = Modifier
                    .weight(.15f)
                    .align(Alignment.CenterVertically),
            ) {
                Column(
                    modifier = Modifier.matchParentHeight(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    if (isNetworkAvailable)
                        Box(
                            modifier = Modifier
                                .size(20.dp)
                                .clip(CircleShape)
                                .background(if (isAvailable) Color.Green else Color.Red)
                        )
                    else
                        Icon(
                            Icons.Outlined.Circle,
                            stringResource(id = R.string.label_common_no_network)
                        )
                    Text(
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth(),
                        text = stringResource(
                            id = if (!isNetworkAvailable) R.string.status_no_network
                            else if (isAvailable) R.string.status_online
                            else R.string.status_offline
                        )
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

@Preview
@Composable
fun MenuDialogPreview() {
    MenuDialog(
        onCancel = { },
        onShowDeleteDialog = { },
        toDirectory = { },
        toWebdavDetail = { },
        finalCb = { },
    )
}

@Preview
@Composable
fun DeleteEntryDialogPreview() {
    DeleteEntryDialog(
        url = "url",
        loginId = "loginId",
        onDelete = { },
        onCancel = { },
        finalCb = { }
    )
}

@Preview
@Composable
fun WebDavCardPreview() {
    WebDavCard(
        model = WebDavModel(
            name = "name",
            url = "url",
            loginId = "loginId"
        ),
        coroutineScope = CoroutineScope(Dispatchers.IO),
        toShowMenuDialog = { },
        refreshCb = { }
    )
}

@Preview
@Composable
fun TopBarPreview() {
    TopBar(openDrawer = { }, onFilterSites = { }) {

    }
}

@Preview
@Composable
fun HomeScreenPreview() {
    HomeScreen(
        openDrawer = { },
        toEditWebDavScreen = {},
        toDirectoryScreen = {}
    )
}