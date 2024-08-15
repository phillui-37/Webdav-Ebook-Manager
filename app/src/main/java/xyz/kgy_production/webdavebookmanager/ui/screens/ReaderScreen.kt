package xyz.kgy_production.webdavebookmanager.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.window.Dialog
import androidx.hilt.navigation.compose.hiltViewModel
import at.bitfire.dav4jvm.exception.HttpException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import xyz.kgy_production.webdavebookmanager.R
import xyz.kgy_production.webdavebookmanager.data.model.BookMetaData
import xyz.kgy_production.webdavebookmanager.data.model.WebDavCacheData
import xyz.kgy_production.webdavebookmanager.data.model.WebDavDirNode
import xyz.kgy_production.webdavebookmanager.data.model.WebDavModel
import xyz.kgy_production.webdavebookmanager.ui.component.GenericEbookView
import xyz.kgy_production.webdavebookmanager.ui.component.ReaderTopBar
import xyz.kgy_production.webdavebookmanager.ui.viewmodel.ReaderViewModel
import xyz.kgy_production.webdavebookmanager.util.BOOK_METADATA_CONFIG_FILENAME
import xyz.kgy_production.webdavebookmanager.util.Logger
import xyz.kgy_production.webdavebookmanager.util.getFileFromWebDav
import xyz.kgy_production.webdavebookmanager.util.isNetworkAvailable
import xyz.kgy_production.webdavebookmanager.util.urlDecode
import xyz.kgy_production.webdavebookmanager.util.writeDataToWebDav
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.util.concurrent.atomic.AtomicInteger

fun getCache(
    bookUrl: String,
    model: WebDavModel
): Pair<WebDavCacheData, Pair<WebDavDirNode, BookMetaData>>? {
    val logger by Logger.delegate("ReaderScr.getCache")
    logger.d("url: $bookUrl")
    return runBlocking(Dispatchers.IO) {
        val url = "${model.url}/$BOOK_METADATA_CONFIG_FILENAME"
        getFileFromWebDav(
            url,
            model.loginId,
            model.password
        )?.let { data ->
            val cacheRoot = Json.decodeFromString<WebDavCacheData>(data.decodeToString())
            val bookPath = bookUrl.urlDecode().replace(model.url, "").split("/")
            val current = bookPath[bookPath.size - 2]
            val relativePath =
                bookPath.subList(0, bookPath.size - 3).joinToString("/").ifEmpty { "/" }
            cacheRoot.dirCache.find {
                it.current == current && it.relativePath == relativePath
            }?.let { dirCache ->
                cacheRoot.bookMetaDataLs.find { it.name == bookPath.last() }?.let { bookMetaData ->
                    cacheRoot to (dirCache to bookMetaData)
                }
            }
        }
    }
}

fun updateCache(cacheData: WebDavCacheData, newBookMetaData: BookMetaData): WebDavCacheData {
    return cacheData.copy(
        bookMetaDataLs = cacheData.bookMetaDataLs.map {
            if (it.fullUrl == newBookMetaData.fullUrl && it.name == newBookMetaData.name && it.fileType == newBookMetaData.fileType)
                newBookMetaData
            else
                it
        },
        lastUpdateTime = LocalDateTime.now()
    )
}

@Composable
fun ReaderScreen(
    webDavId: Int,
    bookUrl: String,
    fromDirUrl: String,
    onBack: () -> Unit,
    viewModel: ReaderViewModel = hiltViewModel(),
) {
    val logger by Logger.delegate("ReaderScr")
    logger.d("webDavId: $webDavId, url: $bookUrl, fromDir: $fromDirUrl")
    // TODO offline support
    val model = remember {
        runBlocking(Dispatchers.IO) { viewModel.getWebDavModel(webDavId) }
            ?: throw RuntimeException("webdav model not found")
    }
    val _cacheData = remember {
        getCache(bookUrl, model) ?: throw RuntimeException("dir and book metadata not found")
    }
    var cacheData = remember { _cacheData.first }
    var bookMetaData by remember { mutableStateOf(_cacheData.second.second) }
    var url by remember { mutableStateOf(bookUrl) }
    val snackBarHostState = remember { SnackbarHostState() }
    val ctx = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var cacheLastUpdateTime by
    remember { mutableLongStateOf(LocalDateTime.now().toEpochSecond(ZoneOffset.UTC)) }
    var updatingToken by remember { mutableStateOf(AtomicInteger()) }
    var isShowBookmarkDialog by remember { mutableStateOf(false) }
    var isShowTagDialog by remember { mutableStateOf(false) }

    DisposableEffect(bookMetaData) {
        onDispose {
            val token = updatingToken.incrementAndGet()
            CoroutineScope(Dispatchers.IO).launch {
                cacheData = updateCache(cacheData, bookMetaData)
                if (!ctx.isNetworkAvailable()) return@launch
                val now = LocalDateTime.now().toEpochSecond(ZoneOffset.UTC)
                val timeDiff = now - cacheLastUpdateTime
                if (timeDiff < 10)
                    delay(timeDiff)
                val tokenNow = updatingToken.get()
                if (token == tokenNow) { // only get the latest update
                    try {
                        writeDataToWebDav(
                            Json.encodeToString(cacheData),
                            BOOK_METADATA_CONFIG_FILENAME,
                            model.url,
                            model.loginId,
                            model.password
                        )
                    } catch (e: HttpException) {
                        if (e.code != 423) {
                            logger.w("Update too fast")
                            throw e
                        }
                    }
                } else {
                    logger.d("$token is not the latest update flow, latest is $tokenNow")
                }
            }
        }
    }

    BackHandler {
        onBack()
    }

    if (isShowBookmarkDialog) {
        BookmarkDialog() {
            isShowBookmarkDialog = false
        }
    }

    if (isShowTagDialog) {
        TagDialog() {
            isShowTagDialog = false
        }
    }

    Scaffold(
        topBar = {
            ReaderTopBar(
                onRefresh = {
                    url = ""
                    CoroutineScope(Dispatchers.IO).launch {
                        delay(1000)
                        url = bookUrl
                    }
                },
                onBack = onBack,
                toShowBookmarkDialog = { isShowBookmarkDialog = true },
                toShowTagDialog = { isShowTagDialog = true }
            )
        },
        snackbarHost = {
            SnackbarHost(hostState = snackBarHostState)
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
        ) {
            GenericEbookView(
                modifier = Modifier
                    .fillMaxSize(),
                fileUrl = url,
                webDavId = webDavId,
                initProgress = bookMetaData.readProgress,
                scrollUpdateCallback = {
                    bookMetaData = bookMetaData.copy(
                        readProgress = it,
                        lastUpdated = LocalDateTime.now()
                    )
                }
            ) {
                coroutineScope.launch {
                    snackBarHostState.showSnackbar(
                        message = it,
                        actionLabel = ctx.getString(R.string.btn_dismiss),
                        duration = SnackbarDuration.Short
                    )
                }
            }
        }
    }
}

@Composable
private fun BookmarkDialog(
    toCloseDialog: () -> Unit
) {
    //todo
    Dialog(onDismissRequest = { toCloseDialog }) {

    }
}

@Composable
private fun TagDialog(
    toCloseDialog: () -> Unit
) {
    //todo
    Dialog(onDismissRequest = { toCloseDialog() }) {

    }
}