package xyz.kgy_production.webdavebookmanager.component

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.material3.DrawerState
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.navigation.compose.rememberNavController
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import xyz.kgy_production.webdavebookmanager.NaviActions
import xyz.kgy_production.webdavebookmanager.R
import xyz.kgy_production.webdavebookmanager.Screens
import xyz.kgy_production.webdavebookmanager.screens.HomeScreen

@Composable
fun AppModalDrawer(
    drawerState: DrawerState,
    currentRoute: String,
    naviActions: NaviActions,
    coroutineScope: CoroutineScope = rememberCoroutineScope(),
    content: @Composable () -> Unit
) {
    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            AppDrawer(
                currentRoute = currentRoute,
                navigateToHome = { naviActions.navigateToHome() },
                navigateToSetting = { naviActions.navigateToSetting() },
                closeDrawer = { coroutineScope.launch { drawerState.close() } }
            )
        }
    ) {
        content()
    }
}

@Composable
private fun AppDrawer(
    currentRoute: String,
    navigateToHome: () -> Unit,
    navigateToSetting: () -> Unit,
    closeDrawer: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxSize()) {
        DrawerHeader()
        DrawerButton(
            painter = painterResource(id = android.R.drawable.btn_default),
            label = stringResource(id = R.string.screen_home_title),
            isSelected = currentRoute == Screens.HOME,
            action = {
                navigateToHome()
                closeDrawer()
            }
        )
        DrawerButton(
            painter = painterResource(id = android.R.drawable.btn_dropdown),
            label = stringResource(id = R.string.screen_setting_title),
            isSelected = currentRoute == Screens.SETTING,
            action = {
                navigateToSetting()
                closeDrawer()
            }
        )
    }
}

@Composable
private fun DrawerHeader(
    modifier: Modifier = Modifier
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = modifier
            .fillMaxWidth()
    ) {
        Image(
            painter = painterResource(id = android.R.drawable.btn_star),
            contentDescription = "Header" /*TODO*/,
        )
        Text(
            text = "Header",
            color = MaterialTheme.colorScheme.surface
        )
    }
}

@Composable
private fun DrawerButton(
    painter: Painter,
    label: String,
    isSelected: Boolean,
    action: () -> Unit,
    modifier: Modifier = Modifier
) {
    val tintColor = if (isSelected) {
        MaterialTheme.colorScheme.secondary
    } else {
        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
    }

    TextButton(
        onClick = action,
        modifier = modifier
            .fillMaxWidth()
    ) {
        Row(
            horizontalArrangement = Arrangement.Start,
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(
                painter = painter,
                contentDescription = null, // decorative
                tint = tintColor
            )
            Spacer(Modifier.width(16.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                color = tintColor
            )
        }
    }
}

/** preview */
@Preview
@Composable
fun AppModalDrawerPreview() {
    val navController = rememberNavController()
    AppModalDrawer(
        drawerState = rememberDrawerState(initialValue = DrawerValue.Open),
        currentRoute = Screens.HOME,
        naviActions = remember(navController) { NaviActions(navController) }
    ) {
        HomeScreen(
            modifier = Modifier,
            openDrawer = {}
        )
    }
}