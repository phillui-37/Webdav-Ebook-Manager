package xyz.kgy_production.webdavebookmanager

import android.net.Uri
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import xyz.kgy_production.webdavebookmanager.util.urlEncode

private object Screens {
    const val HOME = "home"
    const val SETTING = "setting"
    const val DIRECTORY = "directory"
    const val EDIT_WEBDAV_ENTRY = "editWebdavEntry"
    const val EDIT_BOOK_METADATA =
        "editBookMetadata" // TODO name,series w/ #,author,illustrator,tag,read status,md5(readonly),webdav folder path(readonly)
    const val READ_HISTORY = "readHistory" // TODO
    const val READER = "reader"
}

object Path {
    const val HOME = Screens.HOME
    const val SETTING = Screens.SETTING
    const val DIRECTORY =
        "${Screens.DIRECTORY}/{${RouteArgs.Directory.ID}}?${RouteArgs.Directory.TO_DEST}={${RouteArgs.Directory.TO_DEST}}"
    const val EDIT_WEBDAV_ENTRY =
        "${Screens.EDIT_WEBDAV_ENTRY}?${RouteArgs.EditWebDavEntry.UUID}={${RouteArgs.EditWebDavEntry.UUID}}"
    const val READ_HISTORY = Screens.READ_HISTORY
    val READER =
        "${Screens.READER}?" + listOf(
            RouteArgs.Reader.BOOK_URI,
            RouteArgs.Reader.WEBDAV_ID,
            RouteArgs.Reader.FROM_DIR_URL
        ).joinToString("&") { "$it={$it}" }
}

object RouteArgs {
    object Directory {
        const val ID = "id"
        const val TO_DEST = "toDest"
    }

    object EditWebDavEntry {
        const val UUID = "uuid"
    }

    object Reader {
        const val WEBDAV_ID = "webDavId"
        const val BOOK_URI = "bookUri"
        const val FROM_DIR_URL = "fromDirUrl"
    }
}

class NaviActions(private val navHostController: NavHostController) {
    fun navigateToHome() {
        navHostController.navigate(Screens.HOME) {
            popUpTo(navHostController.graph.findStartDestination().id) {
                saveState = true
            }
            launchSingleTop = true
            restoreState = true
        }
    }

    fun navigateToSetting() {
        navHostController.navigate(Screens.SETTING) {
            popUpTo(navHostController.graph.findStartDestination().id) {
                saveState = true
            }
            launchSingleTop = true
            restoreState = true
        }
    }

    fun navigateToDirectory(id: Int, toDest: String? = null) {
        var route = "${Screens.DIRECTORY}/${id}"
        if (!toDest.isNullOrEmpty())
            route += "?${RouteArgs.Directory.TO_DEST}=$toDest"
        navHostController.navigate(route)
    }

    fun navigateToAddWebdavEntry(uuid: String?) {
        navHostController.navigate(
            if (uuid == null)
                Screens.EDIT_WEBDAV_ENTRY
            else "${Screens.EDIT_WEBDAV_ENTRY}?${RouteArgs.EditWebDavEntry.UUID}=${uuid}"
        )
    }

    // TODO what should be book id? md5?
    fun navigateToEditBookMetadata(bookId: String) {
        navHostController.navigate("${Screens.EDIT_BOOK_METADATA}/$bookId")
    }

    // TODO
    fun navigateToReadHistory() {
        navHostController.navigate(Screens.READ_HISTORY) {
            popUpTo(navHostController.graph.findStartDestination().id) {
                saveState = true
            }
            launchSingleTop = true
            restoreState = true
        }
    }

    fun navigateToReader(webDavId: Int, bookUri: Uri, fromDirUrl: String) {
        navHostController.navigate(
            "${Screens.READER}?${RouteArgs.Reader.BOOK_URI}=${
                bookUri.toString().urlEncode()
            }&${RouteArgs.Reader.WEBDAV_ID}=${webDavId}&${RouteArgs.Reader.FROM_DIR_URL}=${fromDirUrl}"
        )
    }
}