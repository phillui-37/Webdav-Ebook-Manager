package xyz.kgy_production.webdavebookmanager

import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController

object Screens {
    const val HOME = "home"
    const val SETTING = "setting"
    const val DIRECTORY = "directory"
    const val EDIT_WEBDAV_ENTRY = "edit webdav entry" // TODO include add
    const val EDIT_BOOK_METADATA = "edit book metadata" // TODO name,series w/ #,author,illustrator,tag,read status,md5(readonly),webdav folder path(readonly)
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
    fun navigateToDirectory(path: String = "") {
        navHostController.navigate(
            if (path.isEmpty())
                Screens.DIRECTORY
            else "${Screens.DIRECTORY}?path=${path}"
        )
    }

    fun navigateToAddWebdavEntry(uuid: String = "") {
        navHostController.navigate(
            if (uuid.isEmpty())
                Screens.EDIT_WEBDAV_ENTRY
            else "${Screens.EDIT_WEBDAV_ENTRY}?id=${uuid}"
        )
    }

    // TODO what should be book id? md5?
    fun navigateToEditBookMetadata(bookId: String) {
        navHostController.navigate("${Screens.EDIT_BOOK_METADATA}/$bookId")
    }

}