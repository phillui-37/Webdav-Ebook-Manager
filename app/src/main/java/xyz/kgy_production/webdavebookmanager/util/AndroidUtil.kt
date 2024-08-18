package xyz.kgy_production.webdavebookmanager.util

import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import androidx.core.content.FileProvider
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import xyz.kgy_production.webdavebookmanager.BuildConfig
import xyz.kgy_production.webdavebookmanager.data.model.WebDavCacheData
import java.io.File

private val logger by Logger.delegate("AndroidUtil")

fun Context.getShareDir() = "$filesDir/share"
fun Context.getExtAssetsDir() = "$filesDir/extAssets"
fun Context.getWebDavCacheDir() = "$filesDir/dirTreeCache"

fun saveFile(fileName: String, path: String, getData: () -> ByteArray): File {
    logger.d("[saveFile] $path/$fileName")
    File(path).mkdirs()
    val file = File(path, fileName)
    if (!file.exists()) {
        file.outputStream().write(getData())
    }
    return file
}

private fun getWebDavFilePath(webDavUuid: String, path: String?): Pair<String, String> {
    val paths = path?.split("/")
    val dir = webDavUuid.let {
        when {
            paths == null || paths.size <= 1 -> it
            else -> "$it/${paths.subList(0, paths.size - 1).joinToString("/")}"
        }
    }
    return dir to "${(paths?.last() ?: "root").ifEmpty { "root" }}.json"
}

fun Context.saveWebDavCache(cache: WebDavCacheData, webDavUuid: String, path: String) {
    val (dir, filename) = getWebDavFilePath(webDavUuid, path)
    saveFile(filename, getWebDavCacheDir() + "/$dir") {
        Json.encodeToString(cache).encodeToByteArray()
    }
}

fun Context.getWebDavCache(webDavUuid: String, path: String): WebDavCacheData? {
    val (dir, filename) = getWebDavFilePath(webDavUuid, path)
    val file = File(getWebDavCacheDir() + "/$dir", filename)
    logger.d("[getWebDavCache] file path: ${file.path}")
    if (!file.exists()) return null
    return Json.decodeFromString(file.readText())
}

fun Context.isWebDavCacheExists(webDavUuid: String) =
    File(getWebDavCacheDir(), "$webDavUuid.json").exists()

fun Context.saveShareFile(data: ByteArray, fileName: String, path: String? = null) =
    saveShareFile(fileName, path) { data }

fun Context.saveShareFile(fileName: String, path: String? = null, getData: () -> ByteArray): File {
    val logger by Logger.delegate(this::class.java)
    val dir = getShareDir().let {
        if (!path.isNullOrEmpty())
            if (path.startsWith("/")) "$it$path" else "$it/$path"
        else
            it
    }
    logger.d("dir to save: $dir, file: $fileName")
    return saveFile(fileName, dir, getData)
}

fun Context.saveExtAssetsFile(data: ByteArray, fileName: String) =
    saveFile(fileName, getExtAssetsDir()) { data }

fun Context.saveExtAssetsFile(fileName: String, getData: () -> ByteArray) =
    saveFile(fileName, getExtAssetsDir(), getData)

fun Context.getExtAssetsFile(fileName: String): File {
    return File(getExtAssetsDir(), fileName)
}

fun Context.removeAllShareFiles() {
    File(getShareDir()).listFiles()?.forEach { it.deleteRecursively() }
}

fun Context.removeWebDavCache(webDavUuid: String) {
    File(getWebDavCacheDir(), "$webDavUuid.json").delete()
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