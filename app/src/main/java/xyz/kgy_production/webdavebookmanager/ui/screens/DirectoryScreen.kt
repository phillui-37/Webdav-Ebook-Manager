package xyz.kgy_production.webdavebookmanager.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FileCopy
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import xyz.kgy_production.webdavebookmanager.R
import xyz.kgy_production.webdavebookmanager.ui.component.CommonTopBar
import xyz.kgy_production.webdavebookmanager.ui.component.DirectoryTopBar
import xyz.kgy_production.webdavebookmanager.ui.theme.INTERNAL_HORIZONTAL_PADDING_MODIFIER
import xyz.kgy_production.webdavebookmanager.ui.theme.INTERNAL_VERTICAL_PADDING_MODIFIER
import xyz.kgy_production.webdavebookmanager.ui.viewmodel.DirectoryViewModel
import xyz.kgy_production.webdavebookmanager.util.BOOK_METADATA_CONFIG_FILENAME
import xyz.kgy_production.webdavebookmanager.util.Logger
import xyz.kgy_production.webdavebookmanager.util.pipe
import xyz.kgy_production.webdavebookmanager.util.urlDecode

// TODO pull to refresh by network
// TODO extract logic to viewmodel

@OptIn(FlowPreview::class)
@Composable
fun DirectoryScreen(
    id: Int,
    toReaderScreen: (String, String) -> Unit,
    onBack: () -> Unit,
    destUrl: String? = null,
    viewModel: DirectoryViewModel = hiltViewModel(),
) {
    val logger by Logger.delegate("DirectoryScreen")

    // TODO search, filter<-need remote data(protobuf)<-tag/series...
    // TODO long press -> rename, add dir, upload file
    // TODO book show tags, read status and progress
    // state from viewmodel
    val currentPath = viewModel.currentPath.collectAsStateWithLifecycle()
    val contentList = viewModel.contentList.collectAsStateWithLifecycle()
    val isLoading = viewModel.isLoading.collectAsStateWithLifecycle()
    val rootConf = viewModel.rootConf.collectAsStateWithLifecycle()
    val dirTree = viewModel.dirTree.collectAsStateWithLifecycle()

    val coroutineScope = rememberCoroutineScope { Dispatchers.IO }
    runBlocking(coroutineScope.coroutineContext) { viewModel.setWebDavModel(id) }
    val ctx = LocalContext.current

    // local state
    val snackBarHostState = remember { SnackbarHostState() }
    var searchList by remember { mutableStateOf<List<DirectoryViewModel.ContentData>?>(null) }
    var filterList by remember { mutableStateOf<List<DirectoryViewModel.ContentData>?>(null) }
    val scrollState = rememberLazyListState()

    // var
    val re = viewModel.model.bypassPattern.map {
        if (it.isRegex) Regex(it.pattern)
        else Regex(".*${it.pattern}.*")
    }
    val byPassPatternFilter: (String) -> Boolean =
        { path -> re.all { !it.matches(path.urlDecode()) } }

    BackHandler { viewModel.goBack(onBack) }

    LaunchedEffect(currentPath.value, dirTree.value) {
        logger.d("path updated: ${currentPath.value}")
        viewModel.setIsLoading(true)
        viewModel.emptyContentList()
        coroutineScope.launch { viewModel.updateDirTree() }
        viewModel.getRemoteContentList(coroutineScope, currentPath.value)
    }

    LaunchedEffect(rootConf.value) {
        viewModel.onRootConfChanged(coroutineScope)
    }

    SideEffect {
        CoroutineScope(Dispatchers.IO).launch {
            viewModel.searchText.debounce(1000).collectLatest {
                searchList = viewModel.onSearchUpdateSearchList(it)
            }
        }
    }


    Scaffold(
        topBar = {
            if (viewModel.isAtRoot)
                CommonTopBar(
                    title = stringResource(id = R.string.screen_dir_title),
                    onBack = onBack,
                    onSearch = viewModel::search
                )
            else
                DirectoryTopBar(
                    title = viewModel.getNonRootTitle(),
                    onBack = onBack,
                    toParentDir = viewModel::toParentDir,
                    onSearch = viewModel::search,
                    onFilter = {
                        // TODO
                    }
                )
        },
        snackbarHost = {
            SnackbarHost(hostState = snackBarHostState)
        }
    ) { padding ->
        if (isLoading.value)
            LinearProgressIndicator(
                modifier = Modifier
                    .padding(padding)
                    .fillMaxWidth()
            )
        LazyColumn(
            modifier = Modifier
                .padding(padding)
                .then(INTERNAL_VERTICAL_PADDING_MODIFIER)
                .then(INTERNAL_HORIZONTAL_PADDING_MODIFIER),
            state = scrollState,
            contentPadding = PaddingValues(4.dp)
        ) {
            ContentList(
                contentList = if (searchList == null && filterList == null)
                    contentList.value
                else if (searchList != null)
                    searchList!!
                else
                    filterList!!,
                byPassPatternFilter = byPassPatternFilter,
                currentPath = currentPath.value,
                onDirClick = {
                    viewModel.onDirClick(it)
                    viewModel.search("")
                },
                onFileClick = { path, fullUrl, mimeType ->
                    viewModel.onFileClick(path, ctx, fullUrl, mimeType, toReaderScreen)
                    viewModel.search("")
                }
            )

        }
    }

    LaunchedEffect(Unit) {
        viewModel.init(destUrl, coroutineScope, ctx, snackBarHostState)
    }
}

private fun LazyListScope.ContentList(
    contentList: List<DirectoryViewModel.ContentData>,
    byPassPatternFilter: (String) -> Boolean,
    currentPath: String,
    onDirClick: (String) -> Unit,
    onFileClick: (String, String, String) -> Unit,
) {
    items(contentList.filter { it.isDir && byPassPatternFilter(it.fullUrl) && it.fullUrl.urlDecode() != currentPath }) { content ->
        ContentRow(content = content, onClick = onDirClick)
    }
    items(contentList.filter { !it.isDir && byPassPatternFilter(it.fullUrl) && it.name != BOOK_METADATA_CONFIG_FILENAME }) { content ->
        ContentRow(content = content) {
            onFileClick(it, content.fullUrl, content.contentType!!.run { "$type/$subtype" })
        }
    }
}

@Composable
private fun ContentRow(content: DirectoryViewModel.ContentData, onClick: (String) -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(10.dp)
            .wrapContentHeight()
            .shadow(5.dp),
        onClick = { onClick(content.fullUrl) }
    ) {
        Row(
            modifier = Modifier.padding(4.dp, 10.dp),
        ) {
            if (content.isDir)
                Icon(
                    Icons.Filled.Folder,
                    stringResource(id = R.string.webdav_content_dir)
                )
            else
                Icon(
                    Icons.Filled.FileCopy,
                    stringResource(id = R.string.webdav_content_file)
                )
            Text(text = content.name)
        }
    }
}

@Composable
private fun FirstTimeSetupDialog(
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    AlertDialog(
        text = { Text(text = stringResource(id = R.string.screen_dir_fst_time_dialog_text)) },
        onDismissRequest = onDismiss,
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(text = stringResource(id = R.string.btn_cancel))
            }
        },
        confirmButton = {
            TextButton(onClick = onConfirm pipe onDismiss) {
                Text(text = stringResource(id = R.string.btn_confirm))
            }
        }
    )
}