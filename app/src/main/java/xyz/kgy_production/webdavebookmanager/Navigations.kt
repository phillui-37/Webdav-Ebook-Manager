package xyz.kgy_production.webdavebookmanager

import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController

object Screens {
    const val HOME = "home"
    const val SETTING = "setting"
    const val DIRECTORY = "directory"
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
        navHostController.navigate(if (path.isEmpty()) Screens.DIRECTORY else "${Screens.DIRECTORY}?path=${path}")
    }
}