package xyz.kgy_production.webdavebookmanager.util

import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.ui.Modifier
import arrow.core.Either
import arrow.core.Option
import arrow.core.getOrElse
import arrow.core.toOption
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.net.UnknownHostException

fun String.encrypt(): Option<String> = when (val result = encrypt(this)) {
    is Either.Right<*> -> result.getOrNull().toOption()
    else -> {
        result.leftOrNull()!!.printStackTrace()
        arrow.core.none()
    }
}

fun String.decrypt(): Option<String> = when (val result = decrypt(this)) {
    is Either.Right<*> -> result.getOrNull()!!.toOption()
    else -> {
        result.leftOrNull()!!.printStackTrace()
        arrow.core.none()
    }
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