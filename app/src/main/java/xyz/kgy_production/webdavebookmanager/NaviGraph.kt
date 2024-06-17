package xyz.kgy_production.webdavebookmanager

import androidx.compose.material3.DrawerState
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import xyz.kgy_production.webdavebookmanager.component.AppModalDrawer
import xyz.kgy_production.webdavebookmanager.screens.HomeScreen
import xyz.kgy_production.webdavebookmanager.screens.SettingScreen
import xyz.kgy_production.webdavebookmanager.viewmodel.ThemeViewModel

@Composable
fun NaviGraph(
    themeViewModel: ThemeViewModel,
    modifier: Modifier = Modifier,
    navController: NavHostController = rememberNavController(),
    coroutineScope: CoroutineScope = rememberCoroutineScope(),
    drawerState: DrawerState = rememberDrawerState(initialValue = DrawerValue.Closed),
    startDestination: String = Screens.HOME,
    naviActions: NaviActions = remember(navController) { NaviActions(navController) }
) {
    val currentNavBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = currentNavBackStackEntry?.destination?.route ?: startDestination

    NavHost(
        navController = navController,
        startDestination = startDestination,
        modifier = modifier,
    ) {
        composable(Screens.HOME) { 
            AppModalDrawer(drawerState, currentRoute, naviActions) {
                HomeScreen(
                    openDrawer = {
                        coroutineScope.launch { drawerState.open() }
                    }
                )
            }
        }
        composable(Screens.SETTING) { 
            AppModalDrawer(drawerState, currentRoute, naviActions) {
                SettingScreen(
                    themeViewModel = themeViewModel,
                    coroutineScope = coroutineScope,
                    openDrawer = {
                        coroutineScope.launch { drawerState.open() }
                    }
                )
            }
        }
    }
}