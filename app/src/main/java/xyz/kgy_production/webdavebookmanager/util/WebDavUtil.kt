package xyz.kgy_production.webdavebookmanager.util

import at.bitfire.dav4jvm.BasicDigestAuthHandler
import at.bitfire.dav4jvm.DavCollection
import at.bitfire.dav4jvm.exception.ConflictException
import at.bitfire.dav4jvm.exception.NotFoundException
import at.bitfire.dav4jvm.property.CreationDate
import at.bitfire.dav4jvm.property.DisplayName
import at.bitfire.dav4jvm.property.GetContentLength
import at.bitfire.dav4jvm.property.GetContentType
import at.bitfire.dav4jvm.property.GetLastModified
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.MediaType
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.RequestBody.Companion.toRequestBody
import xyz.kgy_production.webdavebookmanager.ui.viewmodel.DirectoryViewModel

private val logger by Logger.delegate("WebDavUtil")


private fun getWebDavCollection(url: String, loginId: String, password: String): DavCollection {
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
    logger.d("[getWebDavDirContentList] get '$url' content")
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
        logger.w("$url is not reachable")
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
    overwrite: Boolean = false,
    mimeType: MediaType = MimeType.JSON.toMediaType(),
) {
    logger.d("[writeDataToWebDav], $filename, $url, $loginId, $password, $data")
    val collection = getWebDavCollection("$url/$filename", loginId, password)
    try {
        collection.put(data.toRequestBody(mimeType)) { response ->
            logger.d("[writeDataToWebDav] ${response.code}: ${response.body}")
        }
    } catch (e: ConflictException) {
        if (overwrite) {
            collection.delete { response ->
                logger.d("[writeDataToWebDav] delete $filename with response ${response.code}")
            }
            collection.put(data.toRequestBody(mimeType)) { response ->
                logger.d("[writeDataToWebDav] ${response.code}: ${response.body}")
            }
        } else {
            logger.e("[writeDataToWebDav] file $filename has conflict, cannot write data to it")
        }
    }
}

fun writeDataToWebDav(
    data: String,
    filename: String,
    url: String,
    loginId: String,
    password: String,
    overwrite: Boolean = false,
    mimeType: MediaType = MimeType.JSON.toMediaType()
) {
    logger.d("[writeDataToWebDav], $filename, $url, $loginId, $password, $data")
    val collection = getWebDavCollection("$url/$filename", loginId, password)
    try {
        collection.put(data.toRequestBody(MimeType.JSON.toMediaType())) { response ->
            logger.d("[writeDataToWebDav] ${response.code}: ${response.body}")
        }
    } catch (e: ConflictException) {
        if (overwrite) {
            collection.delete { response ->
                logger.d("[writeDataToWebDav] delete $filename with response ${response.code}")
            }
            collection.put(data.toRequestBody(mimeType)) { response ->
                logger.d("[writeDataToWebDav] ${response.code}: ${response.body}")
            }
        } else {
            logger.e("[writeDataToWebDav] file $filename has conflict, cannot write data to it")
        }
    }
}

fun getFileFromWebDav(
    filename: String,
    url: String,
    loginId: String,
    password: String,
    resultSetter: (ByteArray?) -> Unit,
) {
    val fileurl = "$url/$filename"
    try {
        val collection = getWebDavCollection(fileurl, loginId, password)
        collection.get(accept = "/", headers = null) { response ->
            resultSetter(if (response.code == 200) response.body?.bytes() else null)
        }
    } catch (e: NotFoundException) {
        logger.d("[getFileFromWebDav] file $filename not exists")
        resultSetter(null)
    }
}

fun getFileFromWebDav(
    fullUrl: String,
    loginId: String,
    password: String,
    resultSetter: (ByteArray?) -> Unit,
) {
    try {
        val collection = getWebDavCollection(fullUrl, loginId, password)
        collection.get(accept = "/", headers = null) { response ->
            resultSetter(if (response.code == 200) response.body?.bytes() else null)
        }
    } catch (e: NotFoundException) {
        logger.d("[getFileFromWebDav] file $fullUrl not exists")
        resultSetter(null)
    }
}

fun getFileFromWebDav(
    fullUrl: String,
    loginId: String,
    password: String,
) = try {
    logger.d("[getFileFromWebDav] $fullUrl")
    val collection = getWebDavCollection(fullUrl, loginId, password)
    var result: ByteArray? = null
    collection.get(accept = "/", headers = null) { response ->
        if (response.code == 200)
            result = response.body?.bytes()
    }
    result
} catch (e: NotFoundException) {
    logger.d("[getFileFromWebDav] file $fullUrl not exists")
    null
}
