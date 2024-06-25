package xyz.kgy_production.webdavebookmanager.util

import android.util.Log
import at.bitfire.dav4jvm.BasicDigestAuthHandler
import at.bitfire.dav4jvm.DavCollection
import at.bitfire.dav4jvm.property.CreationDate
import at.bitfire.dav4jvm.property.DisplayName
import at.bitfire.dav4jvm.property.GetContentLength
import at.bitfire.dav4jvm.property.GetContentType
import at.bitfire.dav4jvm.property.GetLastModified
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import xyz.kgy_production.webdavebookmanager.viewmodel.DirectoryViewModel

suspend fun getWebDavDirContentList(
    url: String,
    loginId: String,
    password: String,
    contentListSetter: (List<DirectoryViewModel.ContentData>) -> Unit
) {
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
    val collection = DavCollection(client, url.toHttpUrl())
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

suspend fun checkIsWebDavDomainAvailable(url: String, loginId: String, password: String): Boolean {
    try {
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
        val collection = DavCollection(client, url.toHttpUrl())
        collection.propfind(1, DisplayName.NAME) { _, _ -> }
        return true
    } catch (e: Exception) {
        Log.w("checkIsWebDavDomainAvailable", "$url is not reachable")
        e.printStackTrace()
        return false
    }
}