package xyz.kgy_production.webdavebookmanager.util

import android.util.Log
import at.bitfire.dav4jvm.BasicDigestAuthHandler
import at.bitfire.dav4jvm.DavCollection
import at.bitfire.dav4jvm.exception.NotFoundException
import at.bitfire.dav4jvm.property.CreationDate
import at.bitfire.dav4jvm.property.DisplayName
import at.bitfire.dav4jvm.property.GetContentLength
import at.bitfire.dav4jvm.property.GetContentType
import at.bitfire.dav4jvm.property.GetLastModified
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.RequestBody.Companion.toRequestBody
import xyz.kgy_production.webdavebookmanager.viewmodel.DirectoryViewModel

fun getWebDavCollection(url: String, loginId: String, password: String): DavCollection {
    val authHandler = BasicDigestAuthHandler(
        domain = null,
        username = loginId,
        password = password,
    )
    val client = OkHttpClient.Builder()
        .followRedirects(false)
        .authenticator(authHandler)
        .addNetworkInterceptor(authHandler)
        .build()
    return DavCollection(client, url.toHttpUrl())
}


fun getWebDavDirContentList(
    url: String,
    loginId: String,
    password: String,
    contentListSetter: (List<DirectoryViewModel.ContentData>) -> Unit
) {
    val collection = getWebDavCollection(url, loginId, password)
    val properties = arrayOf(
        DisplayName.NAME,
        GetContentType.NAME,
        GetContentLength.NAME,
        CreationDate.NAME,
        GetLastModified.NAME,
    )
    val result = mutableListOf<DirectoryViewModel.ContentData>()
    collection.propfind(1, *properties) { response, relation ->
        if (response.href.toString() != "$url/")
            result.add(DirectoryViewModel.ContentData.fromResponse(response))
    }
    contentListSetter(result)
}

fun checkIsWebDavDomainAvailable(url: String, loginId: String, password: String): Boolean {
    try {
        val collection = getWebDavCollection(url, loginId, password)
        collection.propfind(1, DisplayName.NAME) { _, _ -> }
        return true
    } catch (e: Exception) {
        Log.w("checkIsWebDavDomainAvailable", "$url is not reachable")
        e.printStackTrace()
        return false
    }
}

fun writeDataToWebDav(
    data: ByteArray,
    filename: String,
    url: String,
    loginId: String,
    password: String,
) {
    val collection = getWebDavCollection("$url/$filename", loginId, password)
    collection.put(data.toRequestBody(MimeType.PROTOBUF.toMediaType())) { response ->
        Log.d("writeBookMetaDatasToWebDav", "${response.code}: ${response.body}")
    }
}

fun writeDataToWebDav(
    data: String,
    filename: String,
    url: String,
    loginId: String,
    password: String,
) {
    val collection = getWebDavCollection("$url/$filename", loginId, password)
    collection.put(data.toRequestBody(MimeType.PROTOBUF.toMediaType())) { response ->
        Log.d("writeBookMetaDatasToWebDav", "${response.code}: ${response.body}")
    }
}

inline fun getFileFromWebDav(
    filename: String,
    url: String,
    loginId: String,
    password: String,
    crossinline resultSetter: (ByteArray?) -> Unit,
) {
    val fileurl = "$url/$filename"
    try {
        val collection = getWebDavCollection(fileurl, loginId, password)
        collection.get(accept = "/", headers = null) { response ->
            resultSetter(if (response.code == 200) response.body?.bytes() else null)
        }
    } catch (e: NotFoundException) {
      Log.d("getFileFromWebDav", "file $filename not exists")
      resultSetter(null)
    }
}