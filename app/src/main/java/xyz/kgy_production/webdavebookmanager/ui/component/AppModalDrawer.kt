package xyz.kgy_production.webdavebookmanager.ui.component

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Home
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.navigation.compose.rememberNavController
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import xyz.kgy_production.webdavebookmanager.LocalIsDarkTheme
import xyz.kgy_production.webdavebookmanager.NaviActions
import xyz.kgy_production.webdavebookmanager.Path
import xyz.kgy_production.webdavebookmanager.R
import xyz.kgy_production.webdavebookmanager.ui.screens.HomeScreen
import xyz.kgy_production.webdavebookmanager.ui.theme.DRAWER_WIDTH_MODIFIER
import xyz.kgy_production.webdavebookmanager.ui.theme.INTERNAL_HORIZONTAL_PADDING_MODIFIER
import xyz.kgy_production.webdavebookmanager.ui.theme.WebdavEbookManagerTheme
import xyz.kgy_production.webdavebookmanager.util.pipe
import xyz.kgy_production.webdavebookmanager.util.primaryDarkColor
import xyz.kgy_production.webdavebookmanager.util.primaryWhiteColor

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
    ) { content() }
}

@Composable
private fun AppDrawer(
    currentRoute: String,
    navigateToHome: () -> Unit,
    navigateToSetting: () -> Unit,
    closeDrawer: () -> Unit,
    modifier: Modifier = Modifier
) {
    val bgColor = MaterialTheme.colorScheme.secondaryContainer
    Column(
        modifier = modifier
            .then(DRAWER_WIDTH_MODIFIER)
            .fillMaxHeight()
            .background(bgColor)
    ) {
        DrawerHeader()
        DrawerButton(
            imageVector = Icons.Filled.Home,
            label = stringResource(id = R.string.screen_home_title),
            isSelected = currentRoute == Path.HOME,
            action = navigateToHome pipe closeDrawer
        )
        DrawerButton(
            imageVector = Icons.Filled.Edit,
            label = stringResource(id = R.string.screen_setting_title),
            isSelected = currentRoute == Path.SETTING,
            action = navigateToSetting pipe closeDrawer
        )
    }
}

@Composable
private fun DrawerHeader(
    modifier: Modifier = Modifier
) {
    val isDarkTheme = LocalIsDarkTheme.current
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = modifier
            .fillMaxWidth()
            .background(if (isDarkTheme) primaryWhiteColor else primaryDarkColor)
            .height(192.dp)
            .then(INTERNAL_HORIZONTAL_PADDING_MODIFIER)
    ) {
        Image(
            imageVector = Icons.Filled.AccountCircle,
            contentDescription = stringResource(id = R.string.drawer_header_title),
        )
        Text(
            text = stringResource(id = R.string.drawer_header_title),
            color = MaterialTheme.colorScheme.surface
        )
    }
}

@Composable
private fun DrawerButton(
    modifier: Modifier = Modifier,
    painter: Painter? = null,
    imageVector: ImageVector? = null,
    label: String,
    isSelected: Boolean,
    action: () -> Unit,
) {
    require(painter != null || imageVector != null)
    require(painter == null || imageVector == null)
    val tintColor = if (isSelected) {
        MaterialTheme.colorScheme.tertiary
    } else {
        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
    }

    TextButton(
        onClick = action,
        modifier = modifier
            .then(INTERNAL_HORIZONTAL_PADDING_MODIFIER)
            .fillMaxWidth()
    ) {
        Row(
            horizontalArrangement = Arrangement.Start,
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            if (painter != null)
                Icon(
                    painter,
                    contentDescription = null, // decorative
                    tint = tintColor,
                )
            else
                Icon(
                    imageVector = imageVector!!,
                    contentDescription = null,
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
    WebdavEbookManagerTheme {
        AppModalDrawer(
            drawerState = rememberDrawerState(initialValue = DrawerValue.Open),
            currentRoute = Path.HOME,
            naviActions = remember(navController) { NaviActions(navController) }
        ) {
            HomeScreen(
                modifier = Modifier,
                toDirectoryScreen = { "" },
                toEditWebDavScreen = { "" },
                openDrawer = {}
            )
        }
    }
}