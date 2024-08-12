package xyz.kgy_production.webdavebookmanager.data.model

import kotlinx.serialization.Serializable
import okhttp3.MediaType
import okhttp3.MediaType.Companion.toMediaType
import xyz.kgy_production.webdavebookmanager.ui.viewmodel.DirectoryViewModel
import xyz.kgy_production.webdavebookmanager.util.Logger
import xyz.kgy_production.webdavebookmanager.util.fallbackMimeTypeMapping
import java.time.LocalDateTime

@Serializable
data class WebDavCacheData(
    val dirCache: List<WebDavDirNode>,
    val bookMetaDataLs: List<BookMetaData>
) {
    private val logger by Logger.delegate(this::class.java)

    data class WebDavDirTreeNode(
        val current: String,
        val parent: WebDavDirTreeNode?,
        val children: List<WebDavDirTreeNode>
    ) {
        private val logger by Logger.delegate(this::class.java)

        // will not remove the webdav domain
        fun search(path: String): WebDavDirTreeNode? {
            logger.d("to search $path")
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

        private fun getWholeParentPath(): String {
            val ret = mutableListOf<String>()
            var _parent = parent
            while (_parent != null) {
                ret.add(_parent.current)
                _parent = _parent.parent
            }
            return ret.reversed().joinToString("/")
        }

        fun toContentData(baseUrl: String, isDir: Boolean) = run {
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
                "$baseUrl${getWholeParentPath()}$current",
                current,
                LocalDateTime.now(),
                LocalDateTime.now(),
                mimeType,
                0L
            )
        }
    }

    fun dirToTree(): WebDavDirTreeNode {
        val root = WebDavDirTreeNode("/", null, mutableListOf())
        val retryList = mutableListOf<WebDavDirNode>()

        fun findNode(targetParent: String, currentNode: WebDavDirTreeNode): WebDavDirTreeNode? {
//            logger.d("${currentNode.parent?.current},${currentNode.current} vs ${targetParent.split("/")[1]}")
            if (currentNode.current == if (targetParent == "/")
                    targetParent
                else
                    targetParent.split("/")[1]
            )
                return currentNode
            return currentNode.children.fold(null as WebDavDirTreeNode?) { acc, webDavDirTreeNode ->
                acc ?: findNode(targetParent, webDavDirTreeNode)
            }
        }

        dirCache.forEach { dir ->
            if (dir.current != "/") {
                findNode(dir.parent!!, root)?.let {
                    (it.children as MutableList).add(
                        WebDavDirTreeNode(
                            dir.current,
                            it,
                            mutableListOf()
                        )
                    )
                } ?: run {
                    logger.e("dirToTree: $dir cannot find parent")
                    retryList.add(dir)
                }
            }
        }

        var oldListCount = 0
        val toExclude = mutableListOf<WebDavDirNode>()
        // fixed point checking
        while (oldListCount != retryList.size && retryList.size > 0) {
            oldListCount = retryList.size
            toExclude.clear()
            retryList.forEach { dir ->
                findNode(dir.parent!!, root)?.let {
                    toExclude.add(dir)
                    (it.children as MutableList).add(
                        WebDavDirTreeNode(
                            dir.current,
                            it,
                            mutableListOf()
                        )
                    )
                } ?: logger.e("dirToTree: retry $dir cannot find parent")
            }
            toExclude.forEach(retryList::remove)
        }

        return root
    }
}
