package xyz.kgy_production.webdavebookmanager

import android.util.Log
import androidx.compose.material3.DrawerState
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import arrow.core.Some
import arrow.core.getOrElse
import arrow.core.none
import arrow.core.toOption
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import xyz.kgy_production.webdavebookmanager.component.AppModalDrawer
import xyz.kgy_production.webdavebookmanager.screens.EditWebDavEntryScreen
import xyz.kgy_production.webdavebookmanager.screens.HomeScreen
import xyz.kgy_production.webdavebookmanager.screens.SettingScreen
import xyz.kgy_production.webdavebookmanager.viewmodel.FnUpdateThemeSetting
import xyz.kgy_production.webdavebookmanager.viewmodel.ThemeViewModel

@Composable
fun NaviGraph(
    updateThemeSetting: FnUpdateThemeSetting,
    modifier: Modifier = Modifier,
    navController: NavHostController = rememberNavController(),
    coroutineScope: CoroutineScope = rememberCoroutineScope(),
    drawerState: DrawerState = rememberDrawerState(initialValue = DrawerValue.Closed),
    startDestination: String = Path.HOME,
    naviActions: NaviActions = remember(navController) { NaviActions(navController) }
) {
    val currentNavBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = currentNavBackStackEntry?.destination?.route ?: startDestination

    NavHost(
        navController = navController,
        startDestination = startDestination,
        modifier = modifier,
    ) {
        composable(Path.HOME) {
            AppModalDrawer(drawerState, currentRoute, naviActions) {
                HomeScreen(
                    toEditWebDavScreen = {
                        naviActions.navigateToAddWebdavEntry(it.getOrNull())
                    },
                    toDirectoryScreen = {
                        naviActions.navigateToDirectory(it.getOrNull())
                    },
                    openDrawer = {
                        coroutineScope.launch { drawerState.open() }
                    }
                )
            }
        }
        composable(Path.SETTING) {
            AppModalDrawer(drawerState, currentRoute, naviActions) {
                SettingScreen(
                    updateThemeSetting = updateThemeSetting,
                    coroutineScope = coroutineScope,
                    openDrawer = {
                        coroutineScope.launch { drawerState.open() }
                    }
                )
            }
        }
        composable(
            Path.EDIT_WEBDAV_ENTRY,
            arguments = listOf(
                navArgument(RouteArgs.EditWebDavEntry.UUID) { type = NavType.StringType; nullable = true }
            )
        ) { entry ->
            val uuid = when (val it = entry.arguments?.getString(RouteArgs.EditWebDavEntry.UUID)) {
                "", null -> none()
                else -> Some(it)
            }
            EditWebDavEntryScreen(
                uuid = uuid,
                onBack = navController::popBackStack
            )
        }
    }
}