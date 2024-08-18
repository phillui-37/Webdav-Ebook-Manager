package xyz.kgy_production.webdavebookmanager.util

import androidx.annotation.DrawableRes
import androidx.appcompat.content.res.AppCompatResources
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.intl.Locale
import androidx.compose.ui.text.toLowerCase
import com.google.accompanist.drawablepainter.rememberDrawablePainter

@Composable
fun getPainterFromDrawable(@DrawableRes resId: Int) =
    rememberDrawablePainter(
        drawable = AppCompatResources.getDrawable(
            LocalContext.current,
            resId
        )
    )

fun fallbackMimeTypeMapping(fileExt: String): String {
    return when (fileExt.toLowerCase(Locale.current)) {
        "azw" -> "application/vnd.amazon.ebook"
        "azw3" -> "application/vnd.amazon.mobi8-ebook"
        "mobi" -> "application/x-mobipocket-ebook"
        "epub" -> "application/epub+zip"
        "pdf" -> "application/pdf"
        else -> "text/plain"
    }
}