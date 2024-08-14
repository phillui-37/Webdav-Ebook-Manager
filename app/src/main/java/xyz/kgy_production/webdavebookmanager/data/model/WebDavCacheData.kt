package xyz.kgy_production.webdavebookmanager.data.model

import kotlinx.serialization.Serializable
import okhttp3.MediaType
import okhttp3.MediaType.Companion.toMediaType
import xyz.kgy_production.webdavebookmanager.ui.viewmodel.DirectoryViewModel
import xyz.kgy_production.webdavebookmanager.util.Logger
import xyz.kgy_production.webdavebookmanager.util.fallbackMimeTypeMapping
import xyz.kgy_production.webdavebookmanager.util.serializer.LocalDateTimeSerializer
import xyz.kgy_production.webdavebookmanager.util.urlDecode
import xyz.kgy_production.webdavebookmanager.util.urlEncode
import java.time.LocalDateTime

@Serializable
data class WebDavCacheData(
    val dirCache: List<WebDavDirNode>,
    val bookMetaDataLs: List<BookMetaData>,
    @Serializable(with = LocalDateTimeSerializer::class)
    val lastUpdateTime: LocalDateTime = LocalDateTime.now(),
) {
    private val logger by Logger.delegate(this::class.java)

    data class WebDavDirTreeNode(
        val current: String,
        val parent: WebDavDirTreeNode?,
        val isDir: Boolean,
        val children: List<WebDavDirTreeNode>
    ) {
        private val logger by Logger.delegate(this::class.java)

        // will not remove the webdav domain
        fun search(path: String): WebDavDirTreeNode? {
//            logger.d("to search $path")
            var nextDir: WebDavDirTreeNode? = null
            var pathLs = path
                .let { if (it.startsWith("/")) it.substring(1, it.length) else it }
                .split("/")
            try {
                while (pathLs.isNotEmpty()) {
                    nextDir = children.first {
                        it.current == pathLs.first()
                    }
                    if (nextDir.current == pathLs.last()) break
                    else {
                        pathLs = pathLs.subList(1, pathLs.size)
                    }
                }
            } catch (_: NoSuchElementException) {

            }
            return nextDir
        }

        fun getWholeParentPath(): String {
            val ret = mutableListOf<String>()
            var _parent = parent
            while (_parent != null) {
                ret.add(_parent.current)
                _parent = _parent.parent
            }
            return ret.reversed().joinToString("/")
        }

        fun toContentData(baseUrl: String) = run {
            val mimeType: MediaType?
            if (isDir) {
                mimeType = null
            } else {
                val fileExt = current.split(".").last()
                mimeType = try {
                    fallbackMimeTypeMapping(fileExt).toMediaType()
                } catch (e: RuntimeException) {
                    null
                }
            }
            DirectoryViewModel.ContentData(
                "$baseUrl${getWholeParentPath()}${current.urlEncode()}",
                current,
                LocalDateTime.now(),
                LocalDateTime.now(),
                mimeType,
                0L
            )
        }
    }

    fun dirToTree(baseUrl: String): WebDavDirTreeNode {
        // dir first
        val dirNodeList = dirCache.map {
            (it.parent to it.children) to
                    WebDavDirTreeNode(it.current, null, true, mutableListOf())
        }.toMutableList()

        for (i in dirNodeList.indices) {
            val currentNode = dirNodeList[i]
            val parentIdx = dirNodeList
                .indexOfFirst {
                    if (currentNode.first.first == "/" || currentNode.first.first == null)
                        currentNode.first.first == it.second.current
                    else {
                        val parentPath = currentNode.first.first!!.split("/").last()
                        parentPath == it.second.current
                    }
                }
            if (parentIdx != -1) {
                dirNodeList[i] = currentNode.copy(
                    currentNode.first,
                    currentNode.second.copy(parent = dirNodeList[parentIdx].second)
                )
                (dirNodeList[parentIdx].second.children as MutableList).add(dirNodeList[i].second)
            }
        }
        dirNodeList.filter { it.second.parent == null && it.first.first != null }
            .forEach { logger.w("dir ${it.second.current} cannot find parent ${it.first.first}") }

        // book then
        bookMetaDataLs
            .forEach { book ->
                val bookNode = WebDavDirTreeNode(book.name, null, false, listOf())
                val parent = book.fullUrl
                    .replace(baseUrl, "")
                    .urlDecode()
                    .split("/")
                    .let { it[it.size - 2] }
                    .ifEmpty { "/" }
                val parentIdx = dirNodeList
                    .indexOfFirst { it.second.current == parent }
                if (parentIdx != -1) {
                    val node = dirNodeList[parentIdx]
                    (node.second.children as MutableList).add(bookNode)
                } else {
                    logger.w("book ${book.name} cannot find parent $parent")
                }
            }
        return dirNodeList.map { it.second }.find { it.current == "/" }!!
    }
}
