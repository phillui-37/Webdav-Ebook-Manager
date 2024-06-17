package xyz.kgy_production.webdavebookmanager.util

import androidx.annotation.DrawableRes
import androidx.appcompat.content.res.AppCompatResources
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import com.google.accompanist.drawablepainter.rememberDrawablePainter

@Composable
fun getPainterFromDrawable(@DrawableRes resId: Int) =
    rememberDrawablePainter(
        drawable = AppCompatResources.getDrawable(
            LocalContext.current,
            resId
        )
    )