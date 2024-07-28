package xyz.kgy_production.webdavebookmanager.util

import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.ui.Modifier
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.net.UnknownHostException
import java.time.Instant
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

fun String.encrypt(): String? = try {
    encrypt(this)
} catch (e: Exception) {
    null
}

fun String.decrypt(): String? = try {
    decrypt(this)
} catch (e: Exception) {
    null
}

fun URL.checkAvailability(): Boolean {
    val httpURLConnection = (openConnection() as HttpURLConnection)
    httpURLConnection.requestMethod = "HEAD"
    return try {
        httpURLConnection.responseCode == HttpURLConnection.HTTP_OK
    } catch (e: IOException) {
        false
    } catch (e: UnknownHostException) {
        false
    }
}

fun Modifier.matchParentWidth() = width(IntrinsicSize.Max)
fun Modifier.matchParentHeight() = height(IntrinsicSize.Max)

fun String.formatDateTime(format: String): LocalDateTime {
    val formatter = DateTimeFormatter.ofPattern(format)
    return LocalDateTime.parse(this, formatter)
}

fun String.formatDateTime(formatter: DateTimeFormatter): LocalDateTime {
    return LocalDateTime.parse(this, formatter)
}

fun Long.toDateTime() =
    LocalDateTime.ofInstant(Instant.ofEpochMilli(this), java.util.TimeZone.getDefault().toZoneId())