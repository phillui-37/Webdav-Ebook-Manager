package xyz.kgy_production.webdavebookmanager.data.model

import android.util.Log
import arrow.core.Option
import arrow.core.Some
import arrow.core.none
import arrow.core.recover
import kotlinx.serialization.Serializable

@Serializable
data class WebDavCacheData(
    val dirCache: List<WebDavDirNode>,
    val bookMetaDataLs: List<BookMetaData>
) {
    data class WebDavDirTreeNode(
        val current: String,
        val parent: WebDavDirTreeNode?,
        val children: List<WebDavDirTreeNode>
    )

    fun dirToTree(): WebDavDirTreeNode {
        var root = WebDavDirTreeNode("/", null, mutableListOf())
        fun findNode(targetParent: String, currentNode: WebDavDirTreeNode = root): Option<WebDavDirTreeNode> {
            if (currentNode.current == targetParent) return Some(currentNode)
            return currentNode.children.fold(none()) { acc, webDavDirTreeNode ->
                acc.recover { findNode(targetParent, webDavDirTreeNode).bind() }
            }
        }
        dirCache.forEach { dir ->
            if (dir.current != "/") {
                findNode(dir.parent!!)
                    .onSome {
                        (it.children as MutableList).add(WebDavDirTreeNode(dir.current, it, mutableListOf()))
                    }
                    .onNone {
                        Log.e("WebDavCacheData::dirToTree", "$dir cannot find parent")
                    }
            }
        }
        return root
    }
}
