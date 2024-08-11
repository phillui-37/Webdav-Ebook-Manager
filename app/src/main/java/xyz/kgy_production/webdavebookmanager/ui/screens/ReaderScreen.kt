package xyz.kgy_production.webdavebookmanager.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
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
import xyz.kgy_production.webdavebookmanager.ui.viewmodel.ReaderViewModel
import xyz.kgy_production.webdavebookmanager.util.BOOK_METADATA_CONFIG_FILENAME
import xyz.kgy_production.webdavebookmanager.util.Logger
import xyz.kgy_production.webdavebookmanager.util.getFileFromWebDav
import xyz.kgy_production.webdavebookmanager.util.urlDecode
import xyz.kgy_production.webdavebookmanager.util.writeDataToWebDav
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.util.concurrent.atomic.AtomicInteger

fun getCache(
    bookUrl: String,
    model: WebDavModel
): Pair<WebDavCacheData, Pair<WebDavDirNode, BookMetaData>>? {
    return runBlocking(Dispatchers.IO) {
        val url = "${model.url}/$BOOK_METADATA_CONFIG_FILENAME"
        getFileFromWebDav(
            url,
            model.loginId,
            model.password
        )?.let { data ->
            val cacheRoot = Json.decodeFromString<WebDavCacheData>(data.decodeToString())
            val bookPath = bookUrl.replace(model.url, "").split("/").map { it.urlDecode() }
            val current = bookPath[bookPath.size - 2]
            val parent = bookPath[bookPath.size - 3].ifEmpty { "/" }
            cacheRoot.dirCache.find {
                it.current == current && it.parent == parent
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
            if (it.relativePath == newBookMetaData.relativePath && it.name == newBookMetaData.name && it.fileType == newBookMetaData.fileType)
                newBookMetaData
            else
                it
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
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
    val cacheData = remember { _cacheData.first }
    val bookMetaData = remember { mutableStateOf(_cacheData.second.second) }
    val url = remember { mutableStateOf(bookUrl) }
    val snackBarHostState = remember { SnackbarHostState() }
    val ctx = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val cacheLastUpdateTime =
        remember { mutableLongStateOf(LocalDateTime.now().toEpochSecond(ZoneOffset.UTC)) }
    val updatingToken = remember { mutableStateOf(AtomicInteger()) }

    DisposableEffect(bookMetaData.value) {
        onDispose {
            val token = updatingToken.value.incrementAndGet()
            CoroutineScope(Dispatchers.IO).launch {
                val now = LocalDateTime.now().toEpochSecond(ZoneOffset.UTC)
                val timeDiff = now - cacheLastUpdateTime.longValue
                if (timeDiff < 10)
                    delay(timeDiff)
                val tokenNow = updatingToken.value.get()
                if (token == tokenNow) { // only get the last update
                    try {
                        writeDataToWebDav(
                            Json.encodeToString(updateCache(cacheData, bookMetaData.value)),
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

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("") },
                actions = {
                    IconButton(onClick = {
                        url.value = ""
                        CoroutineScope(Dispatchers.IO).launch {
                            delay(1000)
                            url.value = bookUrl
                        }
                    }) {
                        Icon(Icons.Filled.Refresh, "Refresh") // TODO i18n
                    }
                    // TODO bookmark, tag edit
                }
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
                fileUrl = url.value,
                webDavId = webDavId,
                initProgress = bookMetaData.value.readProgress,
                scrollUpdateCallback = {
                    bookMetaData.value = bookMetaData.value.copy(
                        readProgress = it
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