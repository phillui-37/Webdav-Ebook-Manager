package xyz.kgy_production.webdavebookmanager.ui.theme

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.dp
import xyz.kgy_production.webdavebookmanager.util.primaryWhiteColor

val INTERNAL_HORIZONTAL_PADDING_MODIFIER = Modifier.padding(horizontal = 5.dp)
val INTERNAL_VERTICAL_PADDING_MODIFIER = Modifier.padding(vertical = 5.dp)
val DRAWER_WIDTH_MODIFIER = Modifier
    .widthIn(max = 250.dp)
val TOP_BAR_WHITE_MODIFIER = Modifier
    .fillMaxWidth()
    .shadow(5.dp)
val TOP_BAR_DARK_MODIFIER = Modifier
    .fillMaxWidth()
    .drawBehind {
        val borderSize = 2.dp.toPx()
        drawLine(
            color = primaryWhiteColor,
            start = Offset(0f, size.height),
            end = Offset(size.width, size.height),
            strokeWidth = borderSize
        )
    }
//    .border(1.dp, primaryWhiteColor)