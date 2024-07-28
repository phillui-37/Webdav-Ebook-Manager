package xyz.kgy_production.webdavebookmanager.util

import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import androidx.core.content.FileProvider
import xyz.kgy_production.webdavebookmanager.BuildConfig
import java.io.File

fun Context.getShareDir() = "$filesDir/share"

fun Context.saveShareFile(data: ByteArray, fileName: String, path: String? = null) =
    saveShareFile(fileName, path) { data }

fun Context.saveShareFile(fileName: String, path: String? = null, getData: () -> ByteArray): File {
    val logger by Logger.delegate(this::class.java)
    val dir = "${getShareDir()}/share".let {
        if (!path.isNullOrEmpty())
            if (path.startsWith("/")) "$it$path" else "$it/$path"
        else
            it
    }
    logger.d("dir to save: $dir")
    File(dir).mkdirs()
    val file = File(dir, fileName)
    if (!file.exists()) {
        file.outputStream().write(getData())
    }
    return file
}

fun Context.removeAllShareFiles() {
    File(getShareDir()).listFiles()?.forEach { it.deleteRecursively() }
}

fun Context.isSystemDarkMode() =
    (resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES

fun Context.isNetworkAvailable(): Boolean {
    val cm = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    val networkCapabilities = cm.activeNetwork ?: return false
    val actNw =
        cm.getNetworkCapabilities(networkCapabilities) ?: return false
    return when {
        actNw.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> true
        actNw.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> true
        actNw.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> true
        else -> false
    }
}

fun Context.isWifiNetwork(): Boolean {
    val cm = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    val networkCapabilities = cm.activeNetwork ?: return false
    val actNw =
        cm.getNetworkCapabilities(networkCapabilities) ?: return false
    return actNw.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)
}


fun Context.openWithExtApp(file: File, mimeType: String) {
    val logger by Logger.delegate(this::class.java)
    val providerUri =
        FileProvider.getUriForFile(this, BuildConfig.APPLICATION_ID + ".provider", file)
    logger.d("[openWithExtApp] fileUri: $providerUri, mimetype: $mimeType")
    try {
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(providerUri, mimeType)
            setFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        startActivity(Intent.createChooser(intent, "Open File With")) // TODO i18n
    } catch (e: Exception) {
        e.printStackTrace()
    }
}