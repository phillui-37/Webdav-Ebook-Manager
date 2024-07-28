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

        override fun shouldInterceptRequest(
            view: WebView?,
            request: WebResourceRequest?
        ): WebResourceResponse? {
            logger.d("${request?.url}")
            if (request?.url != null) {
                try {
                    val urlStr = request.url.toString()
                    if (urlStr.startsWith("${EBOOK_READER_LIB_URL}file://")) {
                        logger.d("local file request received")
                        val fileUri =
                            Uri.parse(urlStr.replace("${EBOOK_READER_LIB_URL}file://", "file://"))
                        logger.d("uri: $fileUri")
                        val file = fileUri.toFile()
                        logger.d("file: size->${file.length()}, type->${file.extension}")
                        val mimeType = ctx.contentResolver.getType(fileUri)
                            .let { MimeTypeMap.getSingleton().getMimeTypeFromExtension(it) }
                            ?: fallbackMimeTypeMapping(file.extension)
                        logger.d("mimeType: $mimeType, file: ${file.absolutePath}")
                        return WebResourceResponse(mimeType, "UTF-8", file.inputStream())
                    } else {
                        return super.shouldInterceptRequest(view, request)
                    }
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