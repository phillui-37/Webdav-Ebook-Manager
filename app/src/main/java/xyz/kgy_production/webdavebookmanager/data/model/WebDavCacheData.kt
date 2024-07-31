package xyz.kgy_production.webdavebookmanager.data.model

import kotlinx.serialization.Serializable
import okhttp3.MediaType.Companion.toMediaType
import xyz.kgy_production.webdavebookmanager.util.Logger
import xyz.kgy_production.webdavebookmanager.viewmodel.DirectoryViewModel
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
        val retryList = mutableListOf<WebDavDirNode>()

        fun findNode(targetParent: String, currentNode: WebDavDirTreeNode): WebDavDirTreeNode? {
//            logger.d("${currentNode.parent?.current}/${currentNode.current} vs $targetParent")
            if (currentNode.current == if (targetParent == "/") targetParent else targetParent.substring(
                    1,
                    targetParent.length
                )
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
