package xyz.kgy_production.webdavebookmanager.service

import android.app.Service
import android.content.Intent
import arrow.core.Option
import arrow.core.raise.option
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.runBlocking
import xyz.kgy_production.webdavebookmanager.data.WebDavRepository
import xyz.kgy_production.webdavebookmanager.data.model.BookMetaData
import xyz.kgy_production.webdavebookmanager.util.BOOK_METADATA_CONFIG_FILENAME
import xyz.kgy_production.webdavebookmanager.util.checkIsWebDavDomainAvailable
import xyz.kgy_production.webdavebookmanager.util.getWebDavDirContentList
import xyz.kgy_production.webdavebookmanager.util.isNetworkAvailable
import xyz.kgy_production.webdavebookmanager.util.writeDataToWebDav
import xyz.kgy_production.webdavebookmanager.viewmodel.DirectoryViewModel
import javax.inject.Inject

@AndroidEntryPoint
class ScanWebDavService : Service() {
    @Inject
    lateinit var webDavRepository: WebDavRepository

    private class Err(msg: String) : RuntimeException("ScanWebDavService: $msg")

    private val contentMapCache = mutableMapOf<String, List<DirectoryViewModel.ContentData>>()

    override fun onBind(intent: Intent?) = null

    private suspend fun execute(id: Int) = option {
        val webDavData = webDavRepository.getEntryById(id).bind()
        if (!checkIsWebDavDomainAvailable(
                webDavData.url,
                webDavData.loginId,
                webDavData.password
            )
        )
            throw Err("${webDavData.url} not reachable")

        val bookMetaDataLs = mutableListOf<BookMetaData>()
        getCheckingList(webDavData.url, webDavData.loginId, webDavData.password)
            .forEach { webdav ->
                contentMapCache[webdav.fullUrl]!!
                    .filter { !it.isDir }
                    .forEach { book ->

                        bookMetaDataLs.add(BookMetaData(
                            name = book.name,
                            fileType = book.contentType?.run { "$type/$subtype" } ?: BookMetaData.NOT_AVAILABLE,
                            relativePath = book.fullUrl
                        ))
                    }
            }

        writeDataToWebDav(
            bookMetaDataLs,
            BOOK_METADATA_CONFIG_FILENAME,
            webDavData.url,
            webDavData.loginId,
            webDavData.password
        )
    }.onNone {
        throw Err("id $id is not valid")
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (!isNetworkAvailable())
            throw Err("Network not available")
        val id = intent?.getIntExtra("id", -1)!!
        if (id == -1)
            throw Err("id not provided")

        runBlocking { execute(id) }

        return START_STICKY
    }

    private suspend fun getCheckingList(
        url: String,
        loginId: String,
        password: String
    ): List<DirectoryViewModel.ContentData> {
        if (contentMapCache.containsKey(url))
            return contentMapCache[url]!!
        var ls = listOf<DirectoryViewModel.ContentData>()
        getWebDavDirContentList(url, loginId, password) {
            ls = it
        }
        contentMapCache[url] = ls
        return ls + ls.filter { it.isDir }
            .flatMap { getCheckingList(it.fullUrl, loginId, password) }
    }
}