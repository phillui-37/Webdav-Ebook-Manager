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
import xyz.kgy_production.webdavebookmanager.data.model.WebDavCacheData
import xyz.kgy_production.webdavebookmanager.data.model.WebDavModel
import xyz.kgy_production.webdavebookmanager.data.repository.WebDavRepository
import xyz.kgy_production.webdavebookmanager.service.ScanWebDavService
import xyz.kgy_production.webdavebookmanager.util.BOOK_METADATA_CONFIG_FILENAME
import xyz.kgy_production.webdavebookmanager.util.Logger
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
                    response.href.toString().run {
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
    private val _showFirstTimeDialog = MutableStateFlow(false)
    val showFirstTimeDialog: StateFlow<Boolean>
        get() = _showFirstTimeDialog
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
                    Json.encodeToString(it),
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
        val _searchList = mutableListOf<DirectoryViewModel.ContentData>()
        while (pendingCheckList.isNotEmpty()) {
            val data = pendingCheckList.removeFirst()
            logger.d("search processing, remains: ${pendingCheckList.size}")
            logger.d("processing node $data")
            if (data.isDir) {
                dirTree.value?.let { tree ->
                    logger.d("tree search")
                    tree.search(data.fullUrl.replace(model.url, ""))
                        ?.let {
                            logger.d("next layer")
                            pendingCheckList.addAll(it.children.map {
                                it.toContentData(model.url)
                            })
                        }
                }
            }
            if (pattern.matches(data.name.toLowerCase(Locale.current)))
                _searchList.add(data)
        }
        return _searchList
    }

    fun disableFirstTimeDialog() {
        _showFirstTimeDialog.value = false
    }

    // biz logic op
    fun init(destUrl: String?, coroutineScope: CoroutineScope, ctx: Context) {
        logger.d("[init] ${model.url}, ${_currentPath.value}")
//        if (model.url == _currentPath.value) return

        if (ctx.isNetworkAvailable()) {
            logger.d("[init] has network")
            oriConf = firstTimeLaunchCheck(model, coroutineScope.coroutineContext) {
                logger.d("[init] first time launch for ${model.url}")
                _showFirstTimeDialog.value = true
            }
            runBlocking(coroutineScope.coroutineContext) {
                if (getLocalDirTreeCache(ctx) == null) {
                    logger.d("[init] save dir tree cache to local")
                    upsertLocalDirTreeCache(ctx, oriConf!!)
                }
            }
        } else {
            logger.d("[init] no network")
            oriConf = runBlocking(coroutineScope.coroutineContext) {
                getLocalDirTreeCache(ctx)!!
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

    fun getNonRootTitle(): String = _currentPath.value.split("/").run { get(size - 1) }.urlDecode()

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

    suspend fun getRemoteContentList(path: String) {
        logger.d("[getRemoteContentList] $path")
        if (path.isEmpty()) return
        getWebDavDirContentList(
            path,
            model.loginId,
            model.password
        ) {
            // TODO need to check network status and conf->current path last updated, not always need to be updated
//                contentList = it
            _isLoading.value = false
            // TODO update conf to latest list if have changes
//                conf?.let { _conf ->
//                    conf = _conf
//                }
            // TODO call update
        }
    }

    fun execFirstTimeSetup(
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
                paths.last().urlDecode(),
                "/${model.uuid}" + paths.subList(0, paths.size - 1).joinToString("/")
                    .replace(model.url, "").urlDecode()
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
}