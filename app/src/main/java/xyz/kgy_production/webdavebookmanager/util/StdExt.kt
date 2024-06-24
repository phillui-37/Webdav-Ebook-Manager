package xyz.kgy_production.webdavebookmanager.util

import android.util.Log
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.ui.Modifier
import arrow.core.getOrElse
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.net.UnknownHostException

fun String.encrypt() = encrypt(this)
    .getOrElse {
        it.printStackTrace()
        ""
    }

fun String.decrypt() = decrypt(this)
    .getOrElse {
        it.printStackTrace()
        ""
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