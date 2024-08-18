package xyz.kgy_production.webdavebookmanager.data.model

import android.content.Context
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import xyz.kgy_production.webdavebookmanager.util.BOOK_METADATA_CONFIG_FILENAME
import xyz.kgy_production.webdavebookmanager.util.Logger
import xyz.kgy_production.webdavebookmanager.util.getFileFromWebDav
import xyz.kgy_production.webdavebookmanager.util.getWebDavCache
import xyz.kgy_production.webdavebookmanager.util.getWebDavDirContentList
import xyz.kgy_production.webdavebookmanager.util.serializer.LocalDateTimeSerializer
import java.time.LocalDateTime

@Serializable
data class WebDavCacheData(
    val dirCache: List<WebDavDirNode>,
    val bookMetaDataLs: List<BookMetaData>,
    @Serializable(with = LocalDateTimeSerializer::class)
    val lastUpdateTime: LocalDateTime = LocalDateTime.now(),
) {
    private val logger by Logger.delegate(this::class.java)

    // not recursive
    fun searchDir(target: String) = dirCache.filter { it.current.contains(target) }
    fun searchFile(fileName: String) = bookMetaDataLs.filter { it.name.contains(fileName) }

    suspend fun recursiveSearchDir(model: WebDavModel, target: String) =
        recursiveSearch(model, target).first

    suspend fun recursiveSearchFile(model: WebDavModel, target: String) =
        recursiveSearch(model, target).second

    suspend fun recursiveSearch(
        model: WebDavModel,
        target: String
    ): Pair<List<WebDavDirNode>, List<BookMetaData>> {
        val fileLs = mutableListOf<BookMetaData>()
        val dirLs = mutableListOf<WebDavDirNode>()
        val pendingNodes = mutableListOf<WebDavCacheData>()

        dirLs.addAll(searchDir(target))
        fileLs.addAll(searchFile(target))
        pendingNodes.add(this)
        while (pendingNodes.isNotEmpty()) {
            val node = pendingNodes.removeFirst()
            node.dirCache.forEach {
                getWebDavDirContentList(
                    if (it.current == "/" && it.relativePath == null)
                        model.url
                    else
                        "${model.url}${it.relativePath ?: ""}/${it.current}",
                    model.loginId,
                    model.password,
                ) { ls ->
                    ls.forEach {
                        if (!it.isDir && it.name.contains(target)) {
                            fileLs.add(BookMetaData(
                                name = it.name,
                                fileType = it.contentType?.run { "$type/$subtype" } ?: "text/plain",
                                fullUrl = it.fullUrl,
                                relativePath = it.fullUrl
                                    .replace(model.url, "")
                                    .replace(it.name, "")
                                    .removeSuffix("/"),
                                lastUpdated = it.lastModifiedDateTime,
                                fileSize = it.fileSize
                            ))
                        }
                        if (it.isDir) {
                            if (it.name.contains(target))
                                dirLs.add(
                                    WebDavDirNode(
                                        it.name,
                                        it.fullUrl.replace(model.url, "")
                                            .replace(it.name, "")
                                            .removeSuffix("/"),
                                        listOf(),
                                        it.lastModifiedDateTime,
                                        it.fullUrl
                                    )
                                )
                            pendingNodes.add(
                                Json.decodeFromString(
                                    getFileFromWebDav(
                                        "${it.fullUrl}/$BOOK_METADATA_CONFIG_FILENAME",
                                        model.loginId,
                                        model.password
                                    )!!.decodeToString()
                                )
                            )
                        }
                    }
                }
            }
        }

        return dirLs to fileLs
    }

    suspend fun asyncRecursiveSearch(
        ctx: Context,
        model: WebDavModel,
        target: String,
        intermediateResultConsumer: (Pair<List<WebDavDirNode>, List<BookMetaData>>) -> Unit,
        doneCb: () -> Unit = {}
    ) {
        val fileLs = mutableListOf<BookMetaData>()
        val dirLs = mutableListOf<WebDavDirNode>()
        val pendingNodes = mutableListOf<WebDavCacheData>()

        dirLs.addAll(searchDir(target))
        fileLs.addAll(searchFile(target))
        pendingNodes.add(this)
        while (pendingNodes.isNotEmpty()) {
            val node = pendingNodes.removeFirst()
            node.dirCache.forEach {
                val data = ctx.getWebDavCache(
                    model.uuid,
                    it.fullUrl.replace(model.url, "")
                )
                if (data == null) return
                data.bookMetaDataLs.forEach { book ->
                    if (book.name.contains(target))
                        fileLs.add(book)
                }
                data.dirCache.forEach { dir ->
                    if (dir.current.contains(target))
                        dirLs.add(it)
                    ctx.getWebDavCache(
                        model.uuid,
                        dir.fullUrl.replace(model.url, "")
                    )?.let(pendingNodes::add)
                }
                intermediateResultConsumer(dirLs to fileLs)
                dirLs.clear()
                fileLs.clear()
            }
        }

        doneCb()
    }

    fun sorted() = WebDavCacheData(
        dirCache = dirCache.sortedBy { it.relativePath },
        bookMetaDataLs = bookMetaDataLs.sortedWith { a, b ->
            when {
                a.relativePath > b.relativePath -> 1
                a.relativePath < b.relativePath -> -1
                a.series > b.series -> 1
                a.series < b.series -> -1
                a.orderInSeries > b.orderInSeries -> 1
                a.orderInSeries < b.orderInSeries -> -1
                a.name > b.name -> 1
                a.name < b.name -> -1
                else -> 0
            }
        }
    )
}
