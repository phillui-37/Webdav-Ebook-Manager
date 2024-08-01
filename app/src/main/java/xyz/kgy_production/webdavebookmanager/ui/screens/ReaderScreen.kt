package xyz.kgy_production.webdavebookmanager.ui.screens

import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.navigation.compose.hiltViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import xyz.kgy_production.webdavebookmanager.R
import xyz.kgy_production.webdavebookmanager.ui.component.GenericEbookView
import xyz.kgy_production.webdavebookmanager.ui.viewmodel.ReaderViewModel
import xyz.kgy_production.webdavebookmanager.util.Logger

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReaderScreen(
    webDavId: Int,
    bookUri: Uri,
    fromDirUrl: String,
    onBack: () -> Unit,
    viewModel: ReaderViewModel = hiltViewModel(),
) {
    val logger by Logger.delegate("ReaderScr")
    logger.d("webDavId: $webDavId, uri: $bookUri, fromDir: $fromDirUrl")
    val model = runBlocking(Dispatchers.IO) { viewModel.getWebDavModel(webDavId) }
    val uri = remember { mutableStateOf(bookUri) }
    val snackBarHostState = remember { SnackbarHostState() }
    val ctx = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    BackHandler {
        onBack()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("") },
                actions = {
                    IconButton(onClick = {
                        uri.value = Uri.EMPTY
                        CoroutineScope(Dispatchers.IO).launch {
                            delay(1000)
                            uri.value = bookUri
                        }
                    }) {
                        Icon(Icons.Filled.Refresh, "Refresh") // TODO i18n
                    }
                    // TODO bookmark,
                }
            )
        },
        snackbarHost = {
            SnackbarHost(hostState = snackBarHostState)
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
        ) {
            GenericEbookView(
                modifier = Modifier
                    .fillMaxSize(),
                fileUri = uri.value,
                scrollUpdateCallback = {
                    // TODO
                }
            ) {
                coroutineScope.launch {
                    snackBarHostState.showSnackbar(
                        message = it,
                        actionLabel = ctx.getString(R.string.btn_dismiss),
                        duration = SnackbarDuration.Short
                    )
                }
            }
        }
    }
}