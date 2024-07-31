package xyz.kgy_production.webdavebookmanager.util

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.view.ViewGroup
import android.webkit.ConsoleMessage
import android.webkit.MimeTypeMap
import android.webkit.WebChromeClient
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.ui.text.intl.Locale
import androidx.compose.ui.text.toLowerCase
import androidx.core.net.toFile
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.request.get
import io.ktor.client.statement.readBytes
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import xyz.kgy_production.webdavebookmanager.MainApplication.Companion.dataStore
import xyz.kgy_production.webdavebookmanager.common.Result
import java.io.File

private fun getChromeClient() = object : WebChromeClient() {
    private val logger by Logger.delegate("WebView")
    override fun onConsoleMessage(message: ConsoleMessage?): Boolean {
        logger.d(
            "[JS Log] ${message?.message()} -- From line " +
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
        else -> throw RuntimeException("File type $fileExt not supported!")
    }
}

private fun getWebViewClient(
    ctx: Context,
    showErrorMsg: (String) -> Unit,
): WebViewClient {
    return object : WebViewClient() {
        private val logger by Logger.delegate("WebView")

        override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
            super.onPageStarted(view, url, favicon)
            logger.d("start loading $url")
        }

        override fun onPageFinished(view: WebView?, url: String?) {
            super.onPageFinished(view, url)
            logger.d("finished loading $url")
        }

        override fun onReceivedError(
            view: WebView?,
            request: WebResourceRequest?,
            error: WebResourceError?
        ) {
            super.onReceivedError(view, request, error)
            logger.e("${request?.url} => ${error?.errorCode}: ${error?.description}")
        }

        override fun onLoadResource(view: WebView?, url: String?) {
            super.onLoadResource(view, url)
            logger.d("Load resource $url")
        }

        private fun localFileHandling(url: String): Result<WebResourceResponse?> {
            if (!url.startsWith("${EBOOK_READER_LIB_URL}file://")) return Result.fail("")
            logger.d("local file request received")
            val fileUri =
                Uri.parse(url.replace("${EBOOK_READER_LIB_URL}file://", "file://"))
            logger.d("uri: $fileUri")
            val file = fileUri.toFile()
            logger.d("file: size->${file.length()}, type->${file.extension}")
            val mimeType = ctx.contentResolver.getType(fileUri)
                .let { MimeTypeMap.getSingleton().getMimeTypeFromExtension(it) }
                ?: fallbackMimeTypeMapping(file.extension)
            logger.d("mimeType: $mimeType, file: ${file.absolutePath}")
            return Result.ok(WebResourceResponse(mimeType, "UTF-8", file.inputStream()))
        }

        private fun cacheAssetFile(url: String, filename: String, decodedCachedList: List<String>) {
            CoroutineScope(Dispatchers.IO).launch {
                HttpClient(CIO).use { client ->
                    ctx.saveExtAssetsFile(client.get(url).readBytes(), filename)
                    ctx.dataStore.edit {
                        it[stringPreferencesKey(ConfigKey.CACHED_ASSET_LIST.name)] =
                            Json.encodeToString(decodedCachedList + listOf(filename))
                    }
                }
            }
        }

        private fun assetFileHandling(url: String): Result<WebResourceResponse?> {
            if (!url.endsWith(".js") && !url.endsWith(".css")) return Result.fail("")
            logger.d("asset file: $url")
            val filename = url.split("/").last()
            return runBlocking(Dispatchers.IO) {
                val cachedList = ctx.dataStore.data
                    .map { it[stringPreferencesKey(ConfigKey.CACHED_ASSET_LIST.name)] }
                    .firstOrNull() ?: "[]"
                val decodedCachedList = Json.decodeFromString<List<String>>(cachedList)
                logger.d("cached assets list: $cachedList")

                var file: File? = null
                if (!decodedCachedList.contains(filename)) {
                    logger.d("$filename not cached")
                    cacheAssetFile(url, filename, decodedCachedList)
                } else {
                    logger.d("$filename cached")
                    file = ctx.getExtAssetsFile(filename)
                }
                file?.let {
                    logger.d("asset uri ${Uri.fromFile(file)}")
                    Result.ok(
                        WebResourceResponse(
                            if (filename.endsWith(".js")) "text/javascript" else "test/css",
                            "UTF-8",
                            it.inputStream()
                        )
                    )
                } ?: Result.fail("")
            }
        }

        override fun shouldInterceptRequest(
            view: WebView?,
            request: WebResourceRequest?
        ): WebResourceResponse? {
            logger.d("shouldInterceptRequest: ${request?.url}")
            // TODO save file to cache and load file from cache
            if (request?.url != null) {
                try {
                    val urlStr = request.url.toString()
                    return localFileHandling(urlStr)
                        .or { assetFileHandling(urlStr) }
                        .or { Result.ok(super.shouldInterceptRequest(view, request)) }
                        .get()
                } catch (e: RuntimeException) {
                    showErrorMsg(e.message!!)
                    return null
                }
            }
            return super.shouldInterceptRequest(view, request)
        }
    }
}

fun getWebView(
    ctx: Context,
    showErrorMsg: (String) -> Unit,
) = WebView(ctx).apply {
    layoutParams = ViewGroup.LayoutParams(
        ViewGroup.LayoutParams.MATCH_PARENT,
        ViewGroup.LayoutParams.MATCH_PARENT
    )
    webChromeClient = getChromeClient()
    webViewClient = getWebViewClient(ctx, showErrorMsg)
    settings.setup()
}

@SuppressLint("SetJavaScriptEnabled")
private fun WebSettings.setup() {
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