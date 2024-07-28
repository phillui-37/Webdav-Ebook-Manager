package xyz.kgy_production.webdavebookmanager.screens

import android.content.Intent
import android.net.Uri
import android.util.Log
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FileCopy
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import xyz.kgy_production.webdavebookmanager.R
import xyz.kgy_production.webdavebookmanager.component.CommonTopBar
import xyz.kgy_production.webdavebookmanager.component.DirectoryTopBar
import xyz.kgy_production.webdavebookmanager.data.model.WebDavCacheData
import xyz.kgy_production.webdavebookmanager.data.model.WebDavModel
import xyz.kgy_production.webdavebookmanager.service.ScanWebDavService
import xyz.kgy_production.webdavebookmanager.ui.theme.INTERNAL_HORIZONTAL_PADDING_MODIFIER
import xyz.kgy_production.webdavebookmanager.ui.theme.INTERNAL_VERTICAL_PADDING_MODIFIER
import xyz.kgy_production.webdavebookmanager.util.BOOK_METADATA_CONFIG_FILENAME
import xyz.kgy_production.webdavebookmanager.util.getFileFromWebDav
import xyz.kgy_production.webdavebookmanager.util.getWebDavDirContentList
import xyz.kgy_production.webdavebookmanager.util.openWithExtApp
import xyz.kgy_production.webdavebookmanager.util.pipe
import xyz.kgy_production.webdavebookmanager.util.saveShareFile
import xyz.kgy_production.webdavebookmanager.util.writeDataToWebDav
import xyz.kgy_production.webdavebookmanager.viewmodel.DirectoryViewModel
import java.io.File
import java.net.URLDecoder
import java.net.URLEncoder
import java.nio.file.Files
import kotlin.io.path.Path

// TODO pull to refresh by network
// TODO extract logic to viewmodel

@Composable
fun DirectoryScreen(
    id: Int,
    onBack: () -> Unit,
    viewModel: DirectoryViewModel = hiltViewModel(),
) {
    // TODO search, filter<-need remote data(protobuf)<-tag/series...
    // TODO long press -> rename, add dir, upload file
    val model = runBlocking(Dispatchers.IO) {
        viewModel.getWebDavModel(id)!!
    }
    var currentPath by remember { mutableStateOf(model.url) }
    var contentList by remember { mutableStateOf<List<DirectoryViewModel.ContentData>>(listOf()) }
    val coroutineScope = rememberCoroutineScope { Dispatchers.IO }
    var isLoading by remember { mutableStateOf(false) }
    var showFirstTimeDialog by remember { mutableStateOf(false) }
    val ctx = LocalContext.current
    val snackBarHostState = remember { SnackbarHostState() }
    var conf by remember { mutableStateOf<WebDavCacheData?>(null) }
    var dirTree by remember { mutableStateOf<WebDavCacheData.WebDavDirTreeNode?>(null) }

    LaunchedEffect(currentPath) {
        Log.d("DirectoryScreen", "path updated: $currentPath")
        isLoading = true
        contentList = listOf()
        coroutineScope.launch {
            dirTree?.let { tree ->
                tree.search(
                    URLEncoder.encode(currentPath, "UTF-8").replace(model.url, "")
                )?.let {
                    contentList = it.children.map { it.toContentData(model.url) }
                }
            }
        }
        coroutineScope.launch {
            getWebDavDirContentList(currentPath, model.loginId, model.password) {
                // TODO need to check network status and conf->current path last updated, not always need to be updated
                contentList = it
                isLoading = false
                // TODO update conf to latest list
                conf?.let { _conf ->
                    conf = _conf
                }
            }
        }
    }

    LaunchedEffect(conf) {
        conf?.let {
            dirTree = it.dirToTree()
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

    if (showFirstTimeDialog)
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

    Scaffold(
        topBar = {
            if (currentPath == model.url)
                CommonTopBar(
                    title = stringResource(id = R.string.screen_dir_title),
                    onBack = onBack,
                    onSearch = {
                        // TODO
                    }
                )
            else
                DirectoryTopBar(
                    title = URLDecoder.decode(
                        currentPath.split("/").run { get(size - 1) },
                        "UTF-8"
                    ),
                    onBack = onBack,
                    toParentDir = {
                        currentPath = currentPath.split("/")
                            .run { take(size - 1) }
                            .joinToString("/")
                    },
                    onSearch = {
                        // TODO
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
            contentPadding = PaddingValues(4.dp)
        ) {
            items(contentList.filter { it.isDir }) { content ->
                ContentRow(content = content) {
                    Log.d("DirScreen", "Dir onclick: $it")
                    currentPath = it
                }
            }
            items(contentList.filter { !it.isDir }) { content ->
                ContentRow(content = content) {
                    Log.d("DirScreen", "File onclick: $it")
                    val paths = it.split("/")
                    val file = ctx.saveShareFile(
                        URLDecoder.decode(paths.last(), "utf-8"),
                        "/${model.uuid}" + URLDecoder.decode(paths.subList(0, paths.size - 1).joinToString("/").replace(model.url, ""), "utf-8")
                    ) {
                        var data: ByteArray = byteArrayOf()
                        runBlocking(Dispatchers.IO) {
                            getFileFromWebDav(content.fullUrl, model.loginId, model.password) {
                                data = it ?: byteArrayOf()
                            }
                        }
                        data
                    }
                    if (model.defaultOpenByThis) {
                        // todo nav to reader screen
                    } else {
                        ctx.openWithExtApp(
                            file,
                            content.contentType!!.run { "$type/$subtype" })
                    }
                }
            }
        }
    }

    LaunchedEffect(Unit) {
        if (model.url == currentPath)
            conf = firstTimeLaunchCheck(model) {
                Log.d("DirScreen", "first time launch for ")
                showFirstTimeDialog = true
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

private fun firstTimeLaunchCheck(
    model: WebDavModel,
    onIsFirstTime: () -> Unit
) = runBlocking(Dispatchers.IO) {
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

    conf
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