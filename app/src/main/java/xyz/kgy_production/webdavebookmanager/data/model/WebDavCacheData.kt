package xyz.kgy_production.webdavebookmanager.data.model

import android.util.Log
import kotlinx.serialization.Serializable
import okhttp3.MediaType.Companion.toMediaType
import xyz.kgy_production.webdavebookmanager.viewmodel.DirectoryViewModel
import java.time.LocalDateTime

@Serializable
data class WebDavCacheData(
    val dirCache: List<WebDavDirNode>,
    val bookMetaDataLs: List<BookMetaData>
) {
    data class WebDavDirTreeNode(
        val current: String,
        val parent: WebDavDirTreeNode?,
        val children: List<WebDavDirTreeNode>
    ) {
        // will not remove the webdav domain
        fun search(path: String): WebDavDirTreeNode? {
            var nextDir: WebDavDirTreeNode? = null
            var pathLs = path.split("/")
            try {
                while (pathLs.isNotEmpty()) {
                    nextDir = children.first { it.current == pathLs.first() }
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
            return "/" + ret.reversed().joinToString("/")
        }

        fun toContentData(baseUrl: String) = run {
            DirectoryViewModel.ContentData(
                "$baseUrl/${getWholeParentPath()}",
                current,
                LocalDateTime.now(),
                LocalDateTime.now(),
                "text/plain".toMediaType(),
                0L
            )
        }
    }

    fun dirToTree(): WebDavDirTreeNode {
        val root = WebDavDirTreeNode("/", null, mutableListOf())
        fun findNode(targetParent: String, currentNode: WebDavDirTreeNode = root): WebDavDirTreeNode? {
            if (currentNode.current == targetParent) return currentNode
            return currentNode.children.fold(null as WebDavDirTreeNode?) { acc, webDavDirTreeNode ->
                acc ?: findNode(targetParent, webDavDirTreeNode)
            }
        }
        dirCache.forEach { dir ->
            if (dir.current != "/") {
                findNode(dir.parent!!)
                    ?.let {
                        (it.children as MutableList).add(
                            WebDavDirTreeNode(
                                dir.current,
                                it,
                                mutableListOf()
                            )
                        )
                    } ?: Log.e("WebDavCacheData::dirToTree", "$dir cannot find parent")
            }
        }
        return root
    }
}
