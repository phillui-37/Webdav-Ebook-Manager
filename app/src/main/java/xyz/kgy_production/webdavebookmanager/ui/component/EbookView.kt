package xyz.kgy_production.webdavebookmanager.ui.component

import android.net.Uri
import android.webkit.JavascriptInterface
import android.webkit.WebView
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.net.toFile
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import xyz.kgy_production.webdavebookmanager.util.EBOOK_READER_LIB_URL
import xyz.kgy_production.webdavebookmanager.util.Logger
import xyz.kgy_production.webdavebookmanager.util.WEBVIEW_COMMON_DELAY
import xyz.kgy_production.webdavebookmanager.util.getWebView
import kotlin.math.roundToInt

/// TODO i18n

private class JsInterface(
    private val updateScrollValue: (Double) -> Unit,
    private val renderCompleteCallback: () -> Unit,
) {
    @JavascriptInterface
    fun updateScrollState(value: Double) {
        updateScrollValue(value)
    }

    @JavascriptInterface
    fun renderComplete() {
        renderCompleteCallback()
    }
}

@Composable
fun GenericEbookView(
    modifier: Modifier = Modifier,
    fileUri: Uri,
    scrollUpdateCallback: ((Double) -> Unit)? = null,
    showErrorMessage: (String) -> Unit,
) {
    val logger by Logger.delegate("GenericEbookView")
    val webViewRef = remember { mutableStateOf<WebView?>(null) }
    // todo init scroll value
    var initScrollValue by remember { mutableDoubleStateOf(.0) }

    fun scrollStateUpdate(newValue: Double) {
        logger.d("scrolled to $newValue")
        scrollUpdateCallback?.invoke(newValue)
        initScrollValue = newValue
    }

    if (fileUri.toString().isEmpty())
        EmptyView()
    else if (fileUri.toString().endsWith(".txt"))
        PureTextView(
            modifier = modifier,
            fileUri = fileUri,
            initScrollValue = initScrollValue,
            updateScrollState = ::scrollStateUpdate
        )
    else
        CommonEbookView(
            modifier = modifier,
            fileUri = fileUri,
            webViewRef = webViewRef,
            initScrollValue = initScrollValue,
            updateScrollState = ::scrollStateUpdate,
            showErrorMessage = showErrorMessage
        )
}

@Composable
private fun CommonEbookView(
    modifier: Modifier = Modifier,
    fileUri: Uri,
    webViewRef: MutableState<WebView?>,
    initScrollValue: Double,
    updateScrollState: (Double) -> Unit,
    showErrorMessage: (String) -> Unit,
) {
    val logger by Logger.delegate("CommonEbookView")
    logger.d("try to render content of $fileUri")
    val ctx = LocalContext.current
    var localFileUrl by remember { mutableStateOf("") }

    LaunchedEffect(localFileUrl, webViewRef.value) {
        logger.d("effect: url => $localFileUrl, webview => ${webViewRef.value.toString()}")
        if (localFileUrl.isNotBlank() && webViewRef.value != null) {
            val script = ctx.assets.open("loadFile.js").bufferedReader().readText()
                .format(
                    "$EBOOK_READER_LIB_URL$localFileUrl",
                    localFileUrl.split("/").last(),
                    WEBVIEW_COMMON_DELAY
                )
            logger.d("ready to render file by script")
            delay(WEBVIEW_COMMON_DELAY.toLong())
            webViewRef.value!!.evaluateJavascript(script) {
                logger.d("run init script done")
            }
        }
    }

    AndroidView(
        modifier = modifier,
        factory = {
            logger.d("init webview")
            if (webViewRef.value == null) {
                logger.d("no valid webview instance")
                webViewRef.value = getWebView(it, showErrorMessage)
            }
            webViewRef.value!!
        },
        update = {
            logger.d("webview updated")
            if (localFileUrl != fileUri.toString()) {
                localFileUrl = fileUri.toString()
                it.addJavascriptInterface(
                    JsInterface(updateScrollState) {
                        CoroutineScope(Dispatchers.Main).launch {
                            if (initScrollValue != .0)
                                webViewRef.value!!.evaluateJavascript(
                                    "reader.view.goToFraction(${initScrollValue});"
                                ) {
                                    logger.d("js scroll to result $it")
                                }
                        }
                    },
                    "Android"
                )
                val htmlContent = ctx.assets.open("reader.html").bufferedReader().readText()
                it.loadDataWithBaseURL(
                    EBOOK_READER_LIB_URL,
                    htmlContent,
                    "text/html",
                    "utf-8",
                    null
                )
            }
        }
    )
}

@Composable
private fun EmptyView() {
    Row(modifier = Modifier.fillMaxSize()) {

    }
}

@Composable
private fun PureTextView(
    modifier: Modifier = Modifier,
    fileUri: Uri,
    initScrollValue: Double,
    updateScrollState: (Double) -> Unit
) {
    val logger by Logger.delegate("TextView")
    logger.d("try to render content of $fileUri")
    val scrollState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()
    val itemsCount = remember { derivedStateOf { scrollState.layoutInfo.totalItemsCount } }

    val contents = fileUri.toFile().readLines()

    LaunchedEffect(itemsCount) {
        if (initScrollValue > 0) {
            val page = (scrollState.layoutInfo.totalItemsCount * initScrollValue).roundToInt()
            logger.d(
                "Scroll to ${initScrollValue}, page: ${page}, total page: ${scrollState.layoutInfo.totalItemsCount}"
            )
            scrollState.scrollToItem(page)
        }
    }

    DisposableEffect(scrollState.isScrollInProgress) {
        onDispose {
            coroutineScope.launch {
                logger.d(
                    "scrolling, ${scrollState.firstVisibleItemIndex} / ${scrollState.layoutInfo.totalItemsCount}"
                )
                updateScrollState(
                    scrollState.firstVisibleItemIndex.toDouble() / scrollState.layoutInfo.totalItemsCount
                )
            }
        }
    }

    if (contents.isEmpty())
        Column(modifier = Modifier) {}
    else
        LazyColumn(
            modifier = Modifier
                .then(modifier)
                .fillMaxSize(),
            state = scrollState
        ) {
            items(contents) {
                Text(
                    modifier = Modifier.fillMaxWidth(),
                    text = it,
                )
            }
        }
}