package xyz.kgy_production.webdavebookmanager.screens

import android.app.job.JobInfo
import android.app.job.JobScheduler
import android.content.ComponentName
import android.content.Context
import android.os.Build
import android.os.PersistableBundle
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
import arrow.core.Some
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import xyz.kgy_production.webdavebookmanager.MainActivity
import xyz.kgy_production.webdavebookmanager.R
import xyz.kgy_production.webdavebookmanager.component.CommonTopBar
import xyz.kgy_production.webdavebookmanager.component.DirectoryTopBar
import xyz.kgy_production.webdavebookmanager.data.model.WebDavCacheData
import xyz.kgy_production.webdavebookmanager.data.model.WebDavModel
import xyz.kgy_production.webdavebookmanager.service.ScanWebDavService
import xyz.kgy_production.webdavebookmanager.ui.theme.INTERNAL_HORIZONTAL_PADDING_MODIFIER
import xyz.kgy_production.webdavebookmanager.ui.theme.INTERNAL_VERTICAL_PADDING_MODIFIER
import xyz.kgy_production.webdavebookmanager.util.BOOK_METADATA_CONFIG_FILENAME
import xyz.kgy_production.webdavebookmanager.util.NotificationChannelEnum
import xyz.kgy_production.webdavebookmanager.util.getFileFromWebDav
import xyz.kgy_production.webdavebookmanager.util.getWebDavDirContentList
import xyz.kgy_production.webdavebookmanager.util.pipe
import xyz.kgy_production.webdavebookmanager.viewmodel.DirectoryViewModel
import java.net.URLDecoder

@Composable
fun DirectoryScreen(
    id: Int,
    onBack: () -> Unit,
    viewModel: DirectoryViewModel = hiltViewModel(),
) {
    // TODO search, filter<-need remote data(protobuf)<-tag/series...
    // TODO long press -> rename
    val model: WebDavModel
    runBlocking(Dispatchers.IO) {
        model = viewModel.getWebDavModel(id).getOrNull()!!
    }
    var currentPath by remember { mutableStateOf(model.url) }
    var contentList by remember { mutableStateOf<List<DirectoryViewModel.ContentData>>(listOf()) }
    val coroutineScope = rememberCoroutineScope { Dispatchers.IO }
    var isLoading by remember { mutableStateOf(false) }
    var showFirstTimeDialog by remember { mutableStateOf(false) }
    val ctx = LocalContext.current
    val snackBarHostState = remember { SnackbarHostState() }

    LaunchedEffect(currentPath) {
        isLoading = true
        contentList = listOf()
        coroutineScope.launch {
            getWebDavDirContentList(currentPath, model.loginId, model.password) {
                contentList = it
                isLoading = false
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
                startScanService(ctx, model.id)
                MainActivity.keepScreenOn()
            }
        )

    Scaffold(
        topBar = {
            if (currentPath == model.url)
                CommonTopBar(
                    title = stringResource(id = R.string.screen_dir_title),
                    onBack = onBack,
                    onSearch = Some {

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
                    currentPath = content.fullUrl
                }
            }
            items(contentList.filter { !it.isDir }) { content ->
                ContentRow(content = content) {
                    // TODO dl and open file
                }
            }
        }
    }

    LaunchedEffect(Unit) {
        if (model.url == currentPath)
            firstTimeLaunchCheck(model) {
                showFirstTimeDialog = true
            }
    }
}

@Composable
private fun ContentRow(content: DirectoryViewModel.ContentData, pathHandler: (String) -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(10.dp)
            .wrapContentHeight()
            .shadow(5.dp),
        onClick = {

        }
    ) {
        Row(
            modifier = Modifier.padding(4.dp, 10.dp)
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
}

private fun startScanService(ctx: Context, id: Int) {
    val scheduler = ctx.getSystemService(JobScheduler::class.java)
    val builder = JobInfo.Builder(NotificationChannelEnum.ScanWebDavService.id, ComponentName(ctx, ScanWebDavService::class.java)).apply {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            setMinimumLatency(0L)
        }
        setRequiredNetworkType(JobInfo.NETWORK_TYPE_UNMETERED)
        setRequiresBatteryNotLow(true)
    }

    val bundle = PersistableBundle()
    bundle.putInt("id", id)
    builder.setExtras(bundle)

    scheduler.schedule(builder.build())
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