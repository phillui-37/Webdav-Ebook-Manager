package xyz.kgy_production.webdavebookmanager.screens

import android.util.Log
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FileCopy
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import xyz.kgy_production.webdavebookmanager.R
import xyz.kgy_production.webdavebookmanager.component.CommonTopBar
import xyz.kgy_production.webdavebookmanager.component.DirectoryTopBar
import xyz.kgy_production.webdavebookmanager.data.model.WebDavModel
import xyz.kgy_production.webdavebookmanager.ui.theme.INTERNAL_HORIZONTAL_PADDING_MODIFIER
import xyz.kgy_production.webdavebookmanager.ui.theme.INTERNAL_VERTICAL_PADDING_MODIFIER
import xyz.kgy_production.webdavebookmanager.util.getWebDavDirContentList
import xyz.kgy_production.webdavebookmanager.viewmodel.DirectoryViewModel
import java.net.URLDecoder

@Composable
fun DirectoryScreen(
    id: Int,
    onBack: () -> Unit,
    viewModel: DirectoryViewModel = hiltViewModel(),
) {
    // TODO search, filter<-need remote data(protobuf)<-tag/series...
    // TODO long press -> rename
    val model: WebDavModel
    runBlocking(Dispatchers.IO) {
        model = viewModel.getWebDavModel(id).getOrNull()!!
    }
    var currentPath by remember { mutableStateOf(model.url) }
    var contentList by remember { mutableStateOf<List<DirectoryViewModel.ContentData>>(listOf()) }
    val coroutineScope = rememberCoroutineScope { Dispatchers.IO }
    var isLoading by remember { mutableStateOf(false) }

    LaunchedEffect(currentPath) {
        isLoading = true
        contentList = listOf()
        coroutineScope.launch {
            getWebDavDirContentList(currentPath, model.loginId, model.password) {
                contentList = it
                isLoading = false
            }
        }
    }

    Scaffold(
        topBar = {
            if (currentPath == model.url)
                CommonTopBar(
                    title = stringResource(id = R.string.screen_dir_title),
                    onBack = onBack
                )
            else
                DirectoryTopBar(
                    title = URLDecoder.decode(
                        currentPath.split("/").run { get(size - 1) },
                        "UTF-8"
                    ),
                    onBack = onBack,
                    toParentDir = {
                        currentPath = currentPath.split("/")
                            .run { take(size - 1) }
                            .joinToString("/")
                    },
                    onSearch = {
                        // TODO
                    },
                    onFilter = {
                        // TODO
                    }
                )
        }
    ) { padding ->
        if (isLoading)
            LinearProgressIndicator(modifier = Modifier
                .padding(padding)
                .fillMaxWidth())
        LazyColumn(
            modifier = Modifier
                .padding(padding)
                .then(INTERNAL_VERTICAL_PADDING_MODIFIER)
                .then(INTERNAL_HORIZONTAL_PADDING_MODIFIER),
            contentPadding = PaddingValues(4.dp)
        ) {
            items(contentList) { content ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(10.dp)
                        .wrapContentHeight()
                        .shadow(5.dp),
                    onClick = {
                        if (content.isDir)
                            currentPath = content.fullUrl
                        // TODO open file
                    }
                ) {
                    Row(
                        modifier = Modifier.padding(4.dp, 10.dp)
                    ) {
                        if (content.isDir)
                            Icon(
                                Icons.Filled.Folder,
                                stringResource(id = R.string.webdav_content_dir)
                            )
                        else
                            Icon(
                                Icons.Filled.FileCopy,
                                stringResource(id = R.string.webdav_content_file)
                            )
                        Text(text = content.name)
                    }
                }
            }
        }
    }
}