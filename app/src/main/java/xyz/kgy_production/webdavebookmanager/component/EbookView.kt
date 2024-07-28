package xyz.kgy_production.webdavebookmanager.component

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.util.Log
import android.view.ViewGroup
import android.webkit.ConsoleMessage
import android.webkit.JavascriptInterface
import android.webkit.MimeTypeMap
import android.webkit.WebChromeClient
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
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
import androidx.compose.runtime.State
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.intl.Locale
import androidx.compose.ui.text.toLowerCase
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.net.toFile
import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.request.get
import io.ktor.client.statement.readBytes
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import kotlin.math.roundToInt

/// TODO i18n

private const val BASE_URL = "https://phillui-37.github.io/foliate-js/"

private val chromeClient = object : WebChromeClient() {
    override fun onConsoleMessage(message: ConsoleMessage?): Boolean {
        Log.d(
            "JS Log", "${message?.message()} -- From line " +
                    "${message?.lineNumber()} of ${message?.sourceId()}"
        )
        return false
    }
}

private fun fallbackMimeTypeMapping(fileExt: String): String {
    return when (fileExt.toLowerCase(Locale.current)) {
        "azw" -> "application/vnd.amazon.ebook"
        "azw3" -> "application/vnd.amazon.mobi8-ebook"
        "mobi" -> "application/x-mobipocket-ebook"
        "epub" -> "application/epub+zip"
        "pdf" -> "application/pdf"
        "txt" -> "text/plain"
        else -> throw RuntimeException("$fileExt not supported!")
    }
}

private fun getWebViewClient(
    ctx: Context,
): WebViewClient {
    return object : WebViewClient() {
        override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
            super.onPageStarted(view, url, favicon)
            Log.d("WebView", "start loading $url")
        }

        override fun onPageFinished(view: WebView?, url: String?) {
            super.onPageFinished(view, url)
            Log.d("WebView", "finished loading $url")
        }

        override fun onReceivedError(
            view: WebView?,
            request: WebResourceRequest?,
            error: WebResourceError?
        ) {
            super.onReceivedError(view, request, error)
            Log.e("WebView", "${request?.url} => ${error?.errorCode}: ${error?.description}")
        }

        override fun onLoadResource(view: WebView?, url: String?) {
            super.onLoadResource(view, url)
            Log.d("WebView", "Load resource $url")
        }

        override fun shouldInterceptRequest(
            view: WebView?,
            request: WebResourceRequest?
        ): WebResourceResponse? {
            Log.d("WebViewClient", "${request?.url}")
            if (request?.url != null) {
                val urlStr = request.url.toString()
                if (urlStr.startsWith("${BASE_URL}file://")) {
                    Log.d("WebViewClient", "local file request received")
                    val fileUri = Uri.parse(urlStr.replace("${BASE_URL}file://", "file://"))
                    Log.d("WebViewClient", "uri: $fileUri")
                    val file = fileUri.toFile()
                    Log.d("WebViewClient", "file: size->${file.length()}, type->${file.extension}")
                    val mimeType = ctx.contentResolver.getType(fileUri)
                        .let { MimeTypeMap.getSingleton().getMimeTypeFromExtension(it) }
                        ?: fallbackMimeTypeMapping(file.extension)
                    Log.d("WebViewClient", "mimeType: $mimeType, file: ${file.absolutePath}")
                    return WebResourceResponse(mimeType, "UTF-8", file.inputStream())
                } else {
                    return super.shouldInterceptRequest(view, request)
                }
            }
            return super.shouldInterceptRequest(view, request)
        }
    }
}

private fun getWebView(
    ctx: Context,
) = WebView(ctx).apply {
    layoutParams = ViewGroup.LayoutParams(
        ViewGroup.LayoutParams.MATCH_PARENT,
        ViewGroup.LayoutParams.MATCH_PARENT
    )
    webChromeClient = chromeClient
    webViewClient = getWebViewClient(ctx)
    settings.setup(ctx)
}

private suspend fun downloadFile(url: String): ByteArray {
    val client = HttpClient(CIO)
    Log.d("downloadFile", "url: $url")
    return client.get(url).readBytes()
}

private fun Context.saveFile(fileName: String, fileContent: ByteArray): String? {
    Log.d("saveFile", "try to save file to $fileName")
    val file = File(filesDir, fileName)
    if (file.exists()) return Uri.fromFile(file).toString()
    try {
        FileOutputStream(file).use { it.write(fileContent) }
        return Uri.fromFile(file).toString()
    } catch (e: IOException) {
        Log.e("saveFile", "cannot write to ${filesDir}/${fileName}")
        return null
    }
}

@SuppressLint("SetJavaScriptEnabled")
private fun WebSettings.setup(ctx: Context) {
    javaScriptEnabled = true
    domStorageEnabled = true
    allowFileAccess = true
    allowContentAccess = true
    allowUniversalAccessFromFileURLs = true

    useWideViewPort = true
    loadWithOverviewMode = true

    displayZoomControls = false
    builtInZoomControls = false
    setSupportZoom(false)
}

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
fun GenericEbookView(fileUrl: String) {
    val webViewRef = remember { mutableStateOf<WebView?>(null) }
    val scrollState = remember { mutableDoubleStateOf(0.6190476190476191) }

    LaunchedEffect(scrollState.doubleValue) {
        Log.d("WebView", "Scrolled to ${scrollState.doubleValue}")
    }

    if (fileUrl.isBlank())
        EmptyView()
    else if (fileUrl.endsWith(".txt"))
        PureTextView(fileUrl = fileUrl, extScrollState = scrollState) {
            scrollState.doubleValue = it
        }
    else
        CommonEbookView(
            fileUrl = fileUrl,
            webViewRef = webViewRef,
            scrollState = scrollState
        ) { scrollState.doubleValue = it }
}

@Composable
fun EmptyView() {
    Row(modifier = Modifier.fillMaxSize()) {}
}

@Composable
private fun CommonEbookView(
    fileUrl: String,
    webViewRef: MutableState<WebView?>,
    scrollState: State<Double>,
    updateScrollState: (Double) -> Unit
) {
    Log.d("WebView", "try to render content of $fileUrl")
    val ctx = LocalContext.current
    var localFileUrl by remember { mutableStateOf("") }

    LaunchedEffect(fileUrl) {
        if (fileUrl.isNotBlank()) {
            Log.d("WebView", "try to fetch file from $fileUrl")
            CoroutineScope(Dispatchers.IO).launch {
                localFileUrl = ctx.saveFile(
                    fileUrl.split('/').last(),
                    downloadFile(fileUrl)
                ) ?: ""
            }
        }
    }

    LaunchedEffect(localFileUrl, webViewRef.value) {
        Log.d("WebView", "effect: url => $localFileUrl, webview => ${webViewRef.value.toString()}")
        if (localFileUrl.isNotBlank() && webViewRef.value != null) {
            val script = ctx.assets.open("loadFile.js").bufferedReader().readText()
                .format("$BASE_URL$localFileUrl", fileUrl.split("/").last())
            Log.d("WebView", "ready to render file by script")
            delay(1000)
            webViewRef.value!!.evaluateJavascript(script) {
                Log.d("WebView", "run script: $it")
            }
            if (scrollState.value == 0.0)
                updateScrollState(.0)
        }
    }

    AndroidView(
        factory = {
            if (webViewRef.value == null) {
                webViewRef.value = getWebView(it)
            }
            webViewRef.value!!
        },
        update = {
            it.addJavascriptInterface(
                JsInterface(updateScrollState) {
                    CoroutineScope(Dispatchers.Main).launch {
                        if (scrollState.value != .0)
                            webViewRef.value!!.evaluateJavascript(
                                "reader.view.goToFraction(${scrollState.value});"
                            ) {}
                    }
                },
                "Android"
            )
            val htmlContent = ctx.assets.open("reader.html").bufferedReader().readText()
            it.loadDataWithBaseURL(
                BASE_URL,
                htmlContent,
                "text/html",
                "utf-8",
                null
            )
        }
    )
}

@Composable
private fun PureTextView(
    fileUrl: String,
    extScrollState: State<Double>,
    updateScrollState: (Double) -> Unit
) {
    Log.d("TextView", "try to render content of $fileUrl")
    val ctx = LocalContext.current
    val scrollState = rememberLazyListState()
    var contents by remember { mutableStateOf<List<String>>(listOf()) }
    val coroutineScope = rememberCoroutineScope()
    var isLoadDone by remember { mutableStateOf(false) }
    val itemsCount = remember { derivedStateOf { scrollState.layoutInfo.totalItemsCount } }

    LaunchedEffect(itemsCount) {
        if (extScrollState.value > 0) {
            val page = (scrollState.layoutInfo.totalItemsCount * extScrollState.value).roundToInt()
            Log.d(
                "TextView",
                "Scroll to ${extScrollState.value}, page: ${page}, total page: ${scrollState.layoutInfo.totalItemsCount}"
            )
            scrollState.scrollToItem(page)
        }
    }

    DisposableEffect(scrollState.isScrollInProgress) {
        onDispose {
            coroutineScope.launch {
                Log.d(
                    "TextView",
                    "scrolling, ${scrollState.firstVisibleItemIndex} / ${scrollState.layoutInfo.totalItemsCount}"
                )
                updateScrollState(
                    scrollState.firstVisibleItemIndex.toDouble() / scrollState.layoutInfo.totalItemsCount
                )
            }
        }
    }


    LaunchedEffect(fileUrl) {
        isLoadDone = false
        val filePath = runBlocking(Dispatchers.IO) {
            ctx.saveFile(
                fileUrl.split("/").last(),
                downloadFile(fileUrl)
            )
        }
        if (filePath == null) {
            Log.w("TextView", "Cannot download file from $fileUrl")
        } else {
            Log.d("TextView", "try to read content from cached file $filePath")
            contents = Uri.parse(filePath).toFile().readLines()
            if (extScrollState.value == 0.0)
                scrollState.scrollToItem(0)
        }
    }

    if (contents.isEmpty())
        Column {}
    else
        LazyColumn(
            modifier = Modifier
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