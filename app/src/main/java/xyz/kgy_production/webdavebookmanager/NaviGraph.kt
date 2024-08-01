package xyz.kgy_production.webdavebookmanager

import android.net.Uri
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
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import xyz.kgy_production.webdavebookmanager.ui.component.AppModalDrawer
import xyz.kgy_production.webdavebookmanager.ui.screens.DirectoryScreen
import xyz.kgy_production.webdavebookmanager.ui.screens.EditWebDavEntryScreen
import xyz.kgy_production.webdavebookmanager.ui.screens.HomeScreen
import xyz.kgy_production.webdavebookmanager.ui.screens.ReaderScreen
import xyz.kgy_production.webdavebookmanager.ui.screens.SettingScreen
import xyz.kgy_production.webdavebookmanager.util.Logger
import xyz.kgy_production.webdavebookmanager.util.urlDecode
import xyz.kgy_production.webdavebookmanager.ui.viewmodel.FnUpdateThemeSetting

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
    val logger by Logger.delegate("NaviGraph")
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
                        naviActions.navigateToAddWebdavEntry(it)
                    },
                    toDirectoryScreen = {
                        naviActions.navigateToDirectory(it)
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
                navArgument(RouteArgs.EditWebDavEntry.UUID) {
                    type = NavType.StringType; nullable = true
                }
            )
        ) { entry ->
            val uuid = when (val it = entry.arguments?.getString(RouteArgs.EditWebDavEntry.UUID)) {
                "", null -> null
                else -> it
            }
            EditWebDavEntryScreen(
                uuid = uuid,
                onBack = navController::popBackStack
            )
        }
        composable(
            Path.DIRECTORY,
            arguments = listOf(
                navArgument(RouteArgs.Directory.ID) { type = NavType.IntType },
                navArgument(RouteArgs.Directory.TO_DEST) {
                    nullable = true; type = NavType.StringType
                }
            )
        ) { entry ->
            val id = entry.arguments?.getInt(RouteArgs.Directory.ID)!!
            val toDest = entry.arguments?.getString(RouteArgs.Directory.TO_DEST)
            DirectoryScreen(
                id = id,
                toReaderScreen = { uri, path ->
                    naviActions.navigateToReader(webDavId = id, bookUri = uri, fromDirUrl = path)
                },
                destUrl = toDest,
                onBack = navController::popBackStack
            )
        }
        composable(
            Path.READER,
            arguments = listOf(
                navArgument(RouteArgs.Reader.BOOK_URI) { type = NavType.StringType },
                navArgument(RouteArgs.Reader.WEBDAV_ID) { type = NavType.IntType },
                navArgument(RouteArgs.Reader.FROM_DIR_URL) { type = NavType.StringType },
            )
        ) { entry ->
            val webDavId = entry.arguments?.getInt(RouteArgs.Reader.WEBDAV_ID)!!
            val bookUri = Uri.parse(
                entry.arguments?.getString(RouteArgs.Reader.BOOK_URI)?.urlDecode(),
            )
            val fromDirUrl = entry.arguments?.getString(RouteArgs.Reader.FROM_DIR_URL)!!
            ReaderScreen(
                webDavId = webDavId,
                bookUri = bookUri,
                fromDirUrl = fromDirUrl,
                onBack = {
                    navController.popBackStack()
                    navController.popBackStack()
                    naviActions.navigateToDirectory(webDavId, fromDirUrl)
                }
            )
        }
    }
}