package xyz.kgy_production.webdavebookmanager

import android.util.Log
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController

private object Screens {
    const val HOME = "home"
    const val SETTING = "setting"
    const val DIRECTORY = "directory"
    const val EDIT_WEBDAV_ENTRY = "editWebdavEntry" // TODO include add
    const val EDIT_BOOK_METADATA = "editBookMetadata" // TODO name,series w/ #,author,illustrator,tag,read status,md5(readonly),webdav folder path(readonly)
}

object Path {
    const val HOME = Screens.HOME
    const val SETTING = Screens.SETTING
    const val DIRECTORY = "${Screens.DIRECTORY}/${RouteArgs.Directory.PATH}={${RouteArgs.Directory.PATH}}"
    const val EDIT_WEBDAV_ENTRY = "${Screens.EDIT_WEBDAV_ENTRY}?${RouteArgs.EditWebDavEntry.UUID}={${RouteArgs.EditWebDavEntry.UUID}}"
}

object RouteArgs {
    object Directory {
        const val PATH = "path"
    }
    object EditWebDavEntry {
        const val UUID = "uuid"
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

    // will not escape the path
    fun navigateToDirectory(path: String?) {
        navHostController.navigate(
            if (path == null)
                Screens.DIRECTORY
            else "${Screens.DIRECTORY}?${RouteArgs.Directory.PATH}=${path}"
        )
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

}