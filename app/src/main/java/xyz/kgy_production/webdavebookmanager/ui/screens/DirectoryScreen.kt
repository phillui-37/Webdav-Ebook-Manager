package xyz.kgy_production.webdavebookmanager.ui.screens

import android.content.Context
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
import androidx.compose.material3.SnackbarDuration
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
import androidx.compose.ui.text.intl.Locale
import androidx.compose.ui.text.toLowerCase
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import xyz.kgy_production.webdavebookmanager.R
import xyz.kgy_production.webdavebookmanager.data.model.WebDavCacheData
import xyz.kgy_production.webdavebookmanager.data.model.WebDavDirNode
import xyz.kgy_production.webdavebookmanager.data.model.WebDavModel
import xyz.kgy_production.webdavebookmanager.service.ScanWebDavService
import xyz.kgy_production.webdavebookmanager.ui.component.CommonTopBar
import xyz.kgy_production.webdavebookmanager.ui.component.DirectoryTopBar
import xyz.kgy_production.webdavebookmanager.ui.theme.INTERNAL_HORIZONTAL_PADDING_MODIFIER
import xyz.kgy_production.webdavebookmanager.ui.theme.INTERNAL_VERTICAL_PADDING_MODIFIER
import xyz.kgy_production.webdavebookmanager.ui.viewmodel.DirectoryViewModel
import xyz.kgy_production.webdavebookmanager.util.BOOK_METADATA_CONFIG_FILENAME
import xyz.kgy_production.webdavebookmanager.util.Logger
import xyz.kgy_production.webdavebookmanager.util.getFileFromWebDav
import xyz.kgy_production.webdavebookmanager.util.getWebDavDirContentList
import xyz.kgy_production.webdavebookmanager.util.openWithExtApp
import xyz.kgy_production.webdavebookmanager.util.pipe
import xyz.kgy_production.webdavebookmanager.util.saveShareFile
import xyz.kgy_production.webdavebookmanager.util.urlDecode
import xyz.kgy_production.webdavebookmanager.util.writeDataToWebDav

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
    // TODO search, filter<-need remote data(protobuf)<-tag/series...
    // TODO long press -> rename, add dir, upload file
    // TODO book show tags, read status and progress
    val model = runBlocking(Dispatchers.IO) {
        viewModel.getWebDavModel(id)!!
    }
    var currentPath by remember { mutableStateOf(destUrl ?: model.url) }
    var contentList by remember { mutableStateOf<List<DirectoryViewModel.ContentData>>(listOf()) }
    val coroutineScope = rememberCoroutineScope { Dispatchers.IO }
    var isLoading by remember { mutableStateOf(false) }
    var showFirstTimeDialog by remember { mutableStateOf(false) }
    val ctx = LocalContext.current
    val snackBarHostState = remember { SnackbarHostState() }
    var initDone by remember { mutableStateOf(false) }
    var oriConf by remember { mutableStateOf<WebDavCacheData?>(null) }
    var rootConf by remember { mutableStateOf<WebDavCacheData?>(null) }
    var currentConf by remember { mutableStateOf<WebDavDirNode?>(null) }
    var dirTree by remember { mutableStateOf<WebDavCacheData.WebDavDirTreeNode?>(null) }
    val re = model.bypassPattern.map {
        if (it.isRegex) Regex(it.pattern)
        else Regex(".*${it.pattern}.*")
    }
    val byPassPatternFilter: (String) -> Boolean =
        { path -> re.all { !it.matches(path.urlDecode()) } }
    val scrollState = rememberLazyListState()
    val logger by Logger.delegate("DirectoryScreen")
    val searchText = MutableStateFlow("")
    var searchList by remember { mutableStateOf<List<DirectoryViewModel.ContentData>?>(null) }
    val filterList by remember { mutableStateOf<List<DirectoryViewModel.ContentData>?>(null) }

    BackHandler {
        if (currentPath == model.url)
            onBack()
        else
            currentPath = currentPath.split("/").run { subList(0, size - 1) }.joinToString("/")
    }

    LaunchedEffect(currentPath, dirTree) {
        logger.d("path updated: $currentPath")
        isLoading = true
        contentList = listOf()
        coroutineScope.launch {
            if (rootConf == null) return@launch
            if (currentPath == model.url) {
                logger.d("get root tree")
                contentList =
                    dirTree?.children?.map { it.toContentData(model.url) } ?: listOf()
            } else {
                logger.d("get branch")
                dirTree?.let { tree ->
                    val pathToSearch = currentPath.replace(model.url, "").ifEmpty { "/" }
                    tree.search(pathToSearch)?.let {
                        logger.d("node found in tree")
                        contentList = it.children.map { it.toContentData(model.url) }
                    }
                }
            }
        }
        coroutineScope.launch {
            getWebDavDirContentList(currentPath, model.loginId, model.password) {
                // TODO need to check network status and conf->current path last updated, not always need to be updated
//                contentList = it
                isLoading = false
                // TODO update conf to latest list if have changes
//                conf?.let { _conf ->
//                    conf = _conf
//                }
            }
        }
        if (currentPath == model.url)
            currentConf = rootConf?.dirCache?.find { it.current == "/" && it.parent == null }
        else {
            val current = currentPath.replace(model.url, "").urlDecode()
            val parent = current.split("/").let { it[it.size - 2] }.ifEmpty { "/" }
            currentConf = rootConf!!.dirCache.find {
                "/${it.current}" == current && it.parent == parent
            }
        }
        logger.d("current conf: $currentConf")
    }

    LaunchedEffect(oriConf, rootConf, initDone) {
        if (!initDone || rootConf == oriConf) return@LaunchedEffect
        rootConf?.let {
            dirTree = it.dirToTree(model.url)
            coroutineScope.launch {
                writeDataToWebDav(
                    Json.encodeToString(it),
                    BOOK_METADATA_CONFIG_FILENAME,
                    model.url,
                    model.loginId,
                    model.password,
                    true,
                )
            }
        }
    }

    SideEffect {
        CoroutineScope(Dispatchers.IO).launch {
            searchText.debounce(500L).collectLatest {
                if (it.isEmpty()) {
                    searchList = null
                } else {
                    val pattern = Regex(".*${it.toLowerCase(Locale.current)}.*")
                    val pendingCheckList = mutableListOf(*contentList.toTypedArray())
                    val _searchList = mutableListOf<DirectoryViewModel.ContentData>()
                    while (pendingCheckList.isNotEmpty()) {
                        val data = pendingCheckList.removeFirst()
                        logger.d("search processing, remains: ${pendingCheckList.size}")
                        logger.d("processing node $data")
                        if (data.isDir) {
                            dirTree?.let { tree ->
                                logger.d("tree search")
                                tree.search(data.fullUrl.replace(model.url, ""))
                                    ?.let {
                                        logger.d("next layer")
                                        pendingCheckList.addAll(it.children.map {
                                            it.toContentData(model.url)
                                        })
                                    }
                            }
                        }
                        if (pattern.matches(data.name.toLowerCase(Locale.current)))
                            _searchList.add(data)
                    }
                    searchList = _searchList
                }
            }
        }
    }

    if (showFirstTimeDialog) {
        FirstTimeSetupDialog(
            onDismiss = { showFirstTimeDialog = false },
            onConfirm = {
                coroutineScope.launch {
                    snackBarHostState
                        .showSnackbar(
                            message = ctx.getString(R.string.snack_start_scan),
                            actionLabel = ctx.getString(R.string.btn_dismiss),
                            duration = SnackbarDuration.Indefinite,
                        )
                }
                ScanWebDavService.startScanService(ctx, model.id)
            }
        )
    }

    Scaffold(
        topBar = {
            if (currentPath == model.url)
                CommonTopBar(
                    title = stringResource(id = R.string.screen_dir_title),
                    onBack = onBack,
                    onSearch = {
                        coroutineScope.launch {
                            searchText.emit(it)
                        }
                    }
                )
            else
                DirectoryTopBar(
                    title = currentPath.split("/").run { get(size - 1) }.urlDecode(),
                    onBack = onBack,
                    toParentDir = {
                        currentPath = currentPath.split("/")
                            .run { take(size - 1) }
                            .joinToString("/")
                    },
                    onSearch = {
                        coroutineScope.launch {
                            searchText.emit(it)
                        }
                    },
                    onFilter = {
                        // TODO
                    }
                )
        },
        snackbarHost = {
            SnackbarHost(hostState = snackBarHostState)
        }
    ) { padding ->
        if (isLoading)
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
                    contentList
                else if (searchList != null)
                    searchList!!
                else
                    filterList!!,
                logger = logger,
                byPassPatternFilter = byPassPatternFilter,
                currentPath = currentPath,
                setCurrentPath = { currentPath = it },
                model = model,
                toReaderScreen = toReaderScreen,
                ctx = ctx
            )

        }
    }

    LaunchedEffect(Unit) {
        if (model.url == currentPath) {
            // TODO cache to local
            coroutineScope.launch {
                oriConf = firstTimeLaunchCheck(model) {
                    logger.d("first time launch for ${model.url}")
                    showFirstTimeDialog = true
                }
                rootConf = oriConf
                dirTree = rootConf?.dirToTree(model.url)
                initDone = true
            }
        }
    }
}

private fun LazyListScope.ContentList(
    contentList: List<DirectoryViewModel.ContentData>,
    logger: Logger,
    byPassPatternFilter: (String) -> Boolean,
    currentPath: String,
    setCurrentPath: (String) -> Unit,
    model: WebDavModel,
    toReaderScreen: (String, String) -> Unit,
    ctx: Context,
) {
    items(contentList.filter { it.isDir && byPassPatternFilter(it.fullUrl) && it.fullUrl.urlDecode() != currentPath }) { content ->
        ContentRow(content = content) {
            logger.d("Dir onclick: $it")
            setCurrentPath(it)
        }
    }
    items(contentList.filter { !it.isDir && byPassPatternFilter(it.fullUrl) && it.name != BOOK_METADATA_CONFIG_FILENAME }) { content ->
        ContentRow(content = content) {
            logger.d("File onclick: $it")
            if (model.defaultOpenByThis) {
                toReaderScreen(it, currentPath)
            } else {
                val paths = it.split("/")
                val file = ctx.saveShareFile(
                    paths.last().urlDecode(),
                    "/${model.uuid}" + paths.subList(0, paths.size - 1).joinToString("/")
                        .replace(model.url, "").urlDecode()
                ) {
                    var data: ByteArray = byteArrayOf()
                    runBlocking(Dispatchers.IO) {
                        getFileFromWebDav(content.fullUrl, model.loginId, model.password) {
                            data = it ?: byteArrayOf()
                        }
                    }
                    data
                }
                ctx.openWithExtApp(
                    file,
                    content.contentType!!.run { "$type/$subtype" })
            }
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

private suspend fun firstTimeLaunchCheck(
    model: WebDavModel,
    onIsFirstTime: () -> Unit
): WebDavCacheData? {
    var conf: WebDavCacheData? = null
    getFileFromWebDav(
        BOOK_METADATA_CONFIG_FILENAME,
        model.url,
        model.loginId,
        model.password
    ) { rawData ->
        conf = rawData?.let { Json.decodeFromString(it.decodeToString()) }
    }
    if (conf == null) {
        onIsFirstTime()
    }

    return conf
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