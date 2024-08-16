package xyz.kgy_production.webdavebookmanager.ui.viewmodel

import android.content.Context
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.ui.text.intl.Locale
import androidx.compose.ui.text.toLowerCase
import androidx.lifecycle.ViewModel
import at.bitfire.dav4jvm.Response
import at.bitfire.dav4jvm.property.CreationDate
import at.bitfire.dav4jvm.property.DisplayName
import at.bitfire.dav4jvm.property.GetContentLength
import at.bitfire.dav4jvm.property.GetContentType
import at.bitfire.dav4jvm.property.GetLastModified
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import okhttp3.MediaType
import xyz.kgy_production.webdavebookmanager.R
import xyz.kgy_production.webdavebookmanager.data.model.BookMetaData
import xyz.kgy_production.webdavebookmanager.data.model.WebDavCacheData
import xyz.kgy_production.webdavebookmanager.data.model.WebDavDirNode
import xyz.kgy_production.webdavebookmanager.data.model.WebDavModel
import xyz.kgy_production.webdavebookmanager.data.repository.WebDavRepository
import xyz.kgy_production.webdavebookmanager.service.ScanWebDavService
import xyz.kgy_production.webdavebookmanager.util.BOOK_METADATA_CONFIG_FILENAME
import xyz.kgy_production.webdavebookmanager.util.Logger
import xyz.kgy_production.webdavebookmanager.util.fallbackMimeTypeMapping
import xyz.kgy_production.webdavebookmanager.util.formatDateTime
import xyz.kgy_production.webdavebookmanager.util.getFileFromWebDav
import xyz.kgy_production.webdavebookmanager.util.getWebDavCache
import xyz.kgy_production.webdavebookmanager.util.getWebDavDirContentList
import xyz.kgy_production.webdavebookmanager.util.isNetworkAvailable
import xyz.kgy_production.webdavebookmanager.util.openWithExtApp
import xyz.kgy_production.webdavebookmanager.util.saveShareFile
import xyz.kgy_production.webdavebookmanager.util.saveWebDavCache
import xyz.kgy_production.webdavebookmanager.util.toDateTime
import xyz.kgy_production.webdavebookmanager.util.urlDecode
import xyz.kgy_production.webdavebookmanager.util.writeDataToWebDav
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import javax.inject.Inject
import kotlin.coroutines.CoroutineContext

@HiltViewModel
class DirectoryViewModel @Inject constructor(
    private val webDavRepository: WebDavRepository,
) : ViewModel() {
    private val logger by Logger.delegate(this::class.java)

    data class ContentData(
        val fullUrl: String,
        val name: String,
        val creationTime: LocalDateTime, // from String, yyyy-mm-ddTHH:MM:SSZ
        val lastModifiedDateTime: LocalDateTime, // from Long
        val contentType: MediaType?,
        val fileSize: Long,
    ) {
        val isDir: Boolean
            get() = contentType == null

        companion object {
            fun fromResponse(response: Response): ContentData {
                var result = ContentData(
                    response.href.toString().urlDecode().run {
                        if (endsWith("/")) substring(0, length - 1)
                        else this
                    },
                    "",
                    LocalDateTime.now(),
                    LocalDateTime.now(),
                    null,
                    0L
                )
                response.propstat.forEach { propstat ->
                    propstat.properties.forEach { property ->
                        result = when (property) {
                            is DisplayName -> result.copy(name = property.displayName ?: "-")
                            is GetContentType -> result.copy(contentType = property.type)
                            is GetContentLength -> result.copy(fileSize = property.contentLength)
                            is CreationDate -> result.copy(
                                creationTime = property.creationDate.formatDateTime(
                                    DateTimeFormatter.ISO_OFFSET_DATE_TIME
                                )
                            )

                            is GetLastModified -> result.copy(
                                lastModifiedDateTime = property.lastModified.toDateTime()
                            )

                            else -> result
                        }
                    }
                }
                return result
            }
        }
    }

    // state and var
    lateinit var model: WebDavModel
        private set
    private val _currentPath = MutableStateFlow("")
    val currentPath: StateFlow<String>
        get() = _currentPath
    private var oriConf: WebDavCacheData? = null
    private val _rootConf = MutableStateFlow<WebDavCacheData?>(null)
    val rootConf: StateFlow<WebDavCacheData?>
        get() = _rootConf
    private val _dirTree = MutableStateFlow<WebDavCacheData.WebDavDirTreeNode?>(null)
    val dirTree: StateFlow<WebDavCacheData.WebDavDirTreeNode?>
        get() = _dirTree
    private var initDone = false
    val isAtRoot: Boolean
        get() = _currentPath.value == model.url
    private val _searchText = MutableStateFlow("")
    val searchText: StateFlow<String>
        get() = _searchText
    private val _contentList = MutableStateFlow<List<ContentData>>(listOf())
    val contentList: StateFlow<List<ContentData>>
        get() = _contentList
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean>
        get() = _isLoading

    // db operation
    suspend fun setWebDavModel(id: Int) {
        model = webDavRepository.getEntryById(id)!!
    }

    // state operation
    fun updateCurrentPath(newPath: String) {
        _currentPath.value = newPath
    }

    fun search(text: String) {
        _searchText.value = text
    }

    fun setIsLoading(value: Boolean) {
        _isLoading.value = value
    }

    fun emptyContentList() {
        _contentList.value = listOf()
    }

    fun onRootConfChanged(coroutineScope: CoroutineScope) {
        logger.d("[onRootConfChanged] start")
        if (isConfNotChanged()) {
            logger.d("[onRootConfChanged] conf not changed")
            return
        }

        _rootConf.value?.let {
            coroutineScope.launch {
                _dirTree.emit(it.dirToTree(model.url))
                writeDataToWebDav(
                    Json.encodeToString(it.copy(lastUpdateTime = LocalDateTime.now())),
                    BOOK_METADATA_CONFIG_FILENAME,
                    model.url,
                    model.loginId,
                    model.password,
                    true,
                )
            }
        }
    }

    fun onSearchUpdateSearchList(text: String): List<ContentData>? {
        logger.d("[onSearchUpdateSearchList] $text")
        if (text.isEmpty()) return null
        val pattern = Regex(".*${text.toLowerCase(Locale.current)}.*")
        val pendingCheckList = mutableListOf(*contentList.value.toTypedArray())
        val doneList = mutableListOf<String>()
        val _searchList = mutableListOf<ContentData>()
        while (pendingCheckList.isNotEmpty()) {
            val data = pendingCheckList.removeFirst()
            if (doneList.contains(data.fullUrl)) continue
//            logger.d("search processing, remains: ${pendingCheckList.size}")
//            logger.d("processing node $data")
            if (data.isDir) {
                dirTree.value?.let { tree ->
//                    logger.d("tree search")
                    tree.search(data.fullUrl.replace(model.url, ""))
                        ?.let {
//                            logger.d("next layer")
                            val parentPath =
                                "${model.url}${it.getWholeParentPath()}${it.current}/"
//                            logger.d("parent path: $parentPath")
                            pendingCheckList.addAll(it.children.map {
                                it.toContentData(parentPath)
                            })
                        }
                }
            }
            if (pattern.matches(data.name.toLowerCase(Locale.current))) {
                logger.d("[onSearchUpdateSearchList] match record found, data: ${data.name}, ${data.fullUrl}")
                _searchList.add(data)
            }
            doneList.add(data.fullUrl)
        }
        return _searchList
    }

    // biz logic op
    fun init(
        destUrl: String?,
        coroutineScope: CoroutineScope,
        ctx: Context,
        snackBarHostState: SnackbarHostState
    ) {
        logger.d("[init] ${model.url}, ${_currentPath.value}")
//        if (model.url == _currentPath.value) return

        var remoteConf: WebDavCacheData? = null
        if (ctx.isNetworkAvailable()) {
            logger.d("[init] has network")
            remoteConf = firstTimeLaunchCheck(model, coroutineScope.coroutineContext) {
                logger.d("[init] first time launch for ${model.url}")
                execFirstTimeSetup(coroutineScope, snackBarHostState, ctx)
            }
        }

        // !!!! only concern about which one is newer, not need to concern manual edit without updating lastUpdateTime
        val localConf = runBlocking(coroutineScope.coroutineContext) {
            getLocalDirTreeCache(ctx)
        }

        logger.d("[init] conf status: local->${localConf == null}, remote->${remoteConf == null}")

        if (localConf == null) {
            // network must be available else can't enter dir screen
            logger.d("[init] no local conf cache")
            oriConf = remoteConf
            if (remoteConf != null) {
                logger.d("[init] save dir tree cache to local")
                coroutineScope.launch {
                    upsertLocalDirTreeCache(ctx, remoteConf)
                }
            }
        } else if (remoteConf != null) {
            logger.d("[init] have local conf cache")
            oriConf = cmpDirCache(localConf, remoteConf) {
                logger.d("[init] local conf is newer")
                coroutineScope.launch {
                    writeDataToWebDav(
                        Json.encodeToString(localConf),
                        BOOK_METADATA_CONFIG_FILENAME,
                        model.url,
                        model.loginId,
                        model.password,
                        true,
                    )
                }
            }
        }

        coroutineScope.launch {
            _rootConf.emit(oriConf)
            _dirTree.emit(oriConf?.dirToTree(model.url))
            _currentPath.emit(destUrl ?: model.url)
            _isLoading.emit(false)
            initDone = true
        }
    }

    fun getNonRootTitle(): String = _currentPath.value.split("/").run { get(size - 1) }

    fun goBack(onBack: () -> Unit) {
        if (isAtRoot) onBack()
        else updateCurrentPath(
            _currentPath.value
                .split("/")
                .run { subList(0, size - 1) }
                .joinToString("/")
        )
    }

    fun isConfNotChanged() = !initDone || _rootConf.value == oriConf

    suspend fun updateDirTree() {
        logger.d("[updateDirTree]")
        if (rootConf.value == null) {
            logger.d("[updateDirTree] null root conf")
            return
        }
        if (isAtRoot) {
            logger.d("get root tree")
            _contentList.emit(
                dirTree.value?.children?.map { it.toContentData(model.url) } ?: listOf()
            )
        } else {
            logger.d("get branch")
            dirTree.value?.let { tree ->
                val pathToSearch =
                    currentPath.value.replace(model.url, "").ifEmpty { "/" }
                tree.search(pathToSearch)?.let {
                    logger.d("node found in tree")
                    _contentList.emit(it.children.map { it.toContentData(model.url) })
                }
            }
        }
    }

    fun getRemoteContentList(ctx: Context, coroutineScope: CoroutineScope, path: String) {
        logger.d("[getRemoteContentList] $path")
        if (path.isEmpty()) return
        coroutineScope.launch {
            _isLoading.emit(true)
            getWebDavDirContentList(
                path,
                model.loginId,
                model.password
            ) { contentList ->
                coroutineScope.launch {
                    _contentList.emit(contentList)
                    _isLoading.emit(false)
                }

                if (oriConf != null) {
                    checkAndUpdateDirTreeNode(ctx, model, coroutineScope, contentList)
                }
            }
        }
    }

    fun toParentDir() {
        _currentPath.value = _currentPath.value
            .split("/")
            .run { take(size - 1) }
            .joinToString("/")
    }

    fun onDirClick(path: String) {
        logger.d("Dir onclick: $path")
        _currentPath.value = path
    }

    fun onFileClick(
        path: String,
        ctx: Context,
        fullUrl: String,
        mimeType: String,
        toReaderScreen: (String, String) -> Unit
    ) {
        logger.d("File onclick: $path")
        if (model.defaultOpenByThis) {
            toReaderScreen(path, _currentPath.value)
        } else {
            val paths = path.split("/")
            val file = ctx.saveShareFile(
                paths.last(),
                "/${model.uuid}" + paths.subList(0, paths.size - 1).joinToString("/")
                    .replace(model.url, "")
            ) {
                var data: ByteArray = byteArrayOf()
                runBlocking(Dispatchers.IO) {
                    getFileFromWebDav(fullUrl, model.loginId, model.password) {
                        data = it ?: byteArrayOf()
                    }
                }
                data
            }
            ctx.openWithExtApp(file, mimeType)
        }
    }

    // private fun
    private fun firstTimeLaunchCheck(
        model: WebDavModel,
        coCtx: CoroutineContext,
        onIsFirstTime: () -> Unit
    ): WebDavCacheData? {
        var conf: WebDavCacheData? = null
        runBlocking(coCtx) {
            getFileFromWebDav(
                BOOK_METADATA_CONFIG_FILENAME,
                model.url,
                model.loginId,
                model.password
            ) { rawData ->
                conf = rawData?.let { Json.decodeFromString(it.decodeToString()) }
            }
        }

        if (conf == null) {
            logger.d("[firstTimeLaunchCheck] no remote conf")
            onIsFirstTime()
        }

        return conf
    }

    private fun getLocalDirTreeCache(ctx: Context): WebDavCacheData? {
        return ctx.getWebDavCache(model.uuid)
    }

    private fun upsertLocalDirTreeCache(ctx: Context, cacheData: WebDavCacheData) {
        ctx.saveWebDavCache(cacheData, model.uuid)
    }

    private fun execFirstTimeSetup(
        coroutineScope: CoroutineScope,
        snackBarHostState: SnackbarHostState,
        ctx: Context
    ) {
        coroutineScope.launch {
            snackBarHostState
                .showSnackbar(
                    message = ctx.getString(R.string.snack_start_scan),
                    actionLabel = ctx.getString(R.string.btn_dismiss),
                    duration = SnackbarDuration.Indefinite,
                )
        }
        ScanWebDavService.startScanService(ctx, model.id)
    }

    private fun cmpDirCache(
        local: WebDavCacheData?,
        remote: WebDavCacheData?,
        updateRemoteCb: (WebDavCacheData) -> Unit
    ): WebDavCacheData? {
        return when {
            local == null && remote == null -> null
            local == null -> remote
            remote == null || local.lastUpdateTime > remote.lastUpdateTime -> {
                updateRemoteCb(local)
                local
            }

            else -> remote
        }
    }

    /**
     * This function only update the same relative path of the whole tree
     */
    private fun checkAndUpdateDirTreeNode(
        ctx: Context,
        webdavModel: WebDavModel,
        coroutineScope: CoroutineScope,
        remoteContentList: List<ContentData>
    ) {
        val conf = oriConf!!
        val dirCacheLs = conf.dirCache.toMutableList()
        val booksMetaData = conf.bookMetaDataLs.toMutableList()
        val newDirs = mutableListOf<WebDavDirNode>()
        val newBooks = mutableListOf<BookMetaData>()

        // content list scan
        remoteContentList.forEach { contentData ->
            val paths = contentData.fullUrl
                .replace(model.url, "")
                .split("/")
            val relativePath = when (paths.size) {
                0 -> null
                1 -> "/"
                else -> paths.subList(0, paths.size - 2).joinToString("/")
                    .let { if (it == "") null else it }
            }  
            val current = paths.last()
            if (contentData.isDir) {
                // dir part
                val dirNode = WebDavDirNode(current, relativePath, listOf(), LocalDateTime.now())
                val existingNode =
                    dirCacheLs.find { it.current == current && it.relativePath == relativePath }
                if (existingNode == null) newDirs.add(dirNode)
                searchDirAndAppendChild(dirNode.current, dirCacheLs) { node ->
                    relativePath?.let { it == node.relativePath } ?: false
                }
            } else {
                // book part
                val targetBook = booksMetaData
                    .find {
                        it.fullUrl == contentData.fullUrl
                                && it.name == contentData.name
                                && it.relativePath == (relativePath ?: "/")
                    }
                if (targetBook == null) {
                    val metadata = BookMetaData(
                        name = contentData.name,
                        fileType = fallbackMimeTypeMapping(current.split(".").last()),
                        fullUrl = contentData.fullUrl,
                        relativePath = relativePath ?: "/",
                        lastUpdated = LocalDateTime.now(),
                    )
                    newBooks.add(metadata)
                }
            }
        }

        if (newDirs.size == 0 && newBooks.size == 0) return
        dirCacheLs.forEach { existingDir ->
            newDirs.find { it.relativePath == existingDir.relativePath }
                ?: { newDirs.add(existingDir) }
        }

        // book add back to dir
        newBooks.forEach { book ->
            val checker: (WebDavDirNode) -> Boolean = {
                (it.relativePath ?: "") + "/" + book.name == book.relativePath
            }
            searchDirAndAppendChild(book.name, newDirs, checker)
        }
        booksMetaData.forEach { book ->
            val checker: (WebDavDirNode) -> Boolean = {
                (it.relativePath ?: "") + "/" + book.name == book.relativePath
            }
            searchDirAndAppendChild(book.name, newDirs, checker)
        }

        val newConf =
            conf.copy(dirCache = newDirs, bookMetaDataLs = booksMetaData + newBooks).sorted()

        ctx.saveWebDavCache(newConf, webdavModel.uuid)
        coroutineScope.launch {
            writeDataToWebDav(
                Json.encodeToString(newConf),
                BOOK_METADATA_CONFIG_FILENAME,
                webdavModel.url,
                webdavModel.name,
                webdavModel.password
            )
        }
    }

    private fun searchDirAndAppendChild(
        name: String,
        dirList: MutableList<WebDavDirNode>,
        predicate: (WebDavDirNode) -> Boolean
    ) {
        val idx = dirList.indexOfFirst(predicate)
        if (idx == -1 || dirList[idx].children.contains(name)) return
        dirList[idx] =
            dirList[idx].copy(children = listOf(*dirList[idx].children.toTypedArray(), name))
    }
}