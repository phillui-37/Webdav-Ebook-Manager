package xyz.kgy_production.webdavebookmanager.service

import android.app.job.JobParameters
import android.app.job.JobService
import android.util.Log
import arrow.core.Option
import arrow.core.getOrElse
import arrow.core.raise.option
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.async
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import xyz.kgy_production.webdavebookmanager.MainActivity
import xyz.kgy_production.webdavebookmanager.data.WebDavRepository
import xyz.kgy_production.webdavebookmanager.data.model.BookMetaData
import xyz.kgy_production.webdavebookmanager.data.model.WebDavCacheData
import xyz.kgy_production.webdavebookmanager.data.model.WebDavDirNode
import xyz.kgy_production.webdavebookmanager.data.model.WebDavModel
import xyz.kgy_production.webdavebookmanager.util.BOOK_METADATA_CONFIG_FILENAME
import xyz.kgy_production.webdavebookmanager.util.checkIsWebDavDomainAvailable
import xyz.kgy_production.webdavebookmanager.util.getWebDavDirContentList
import xyz.kgy_production.webdavebookmanager.util.isNetworkAvailable
import xyz.kgy_production.webdavebookmanager.util.writeDataToWebDav
import xyz.kgy_production.webdavebookmanager.viewmodel.DirectoryViewModel
import java.net.URLDecoder
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicInteger
import javax.inject.Inject

@AndroidEntryPoint
class ScanWebDavService : JobService() {
    @Inject
    lateinit var webDavRepository: WebDavRepository

    private class Err(msg: String) : RuntimeException("ScanWebDavService: $msg")

    private data class GetCheckListParam(
        val url: String,
        val loginId: String,
        val password: String,
    ) {
        fun getList(setter: (List<DirectoryViewModel.ContentData>) -> Unit) {
            getWebDavDirContentList(url, loginId, password, setter)
        }
    }

    private lateinit var baseUrl: String
    private val pendingCheckingQueue = Channel<GetCheckListParam>()
    private val fileListQueue = Channel<DirectoryViewModel.ContentData>()
    private val bookMetaDataLs = mutableListOf<BookMetaData>()
    private val bookMetaDataLsMutex = Mutex()
    private val fileCount = AtomicInteger(0)
    private val doneFileCount = AtomicInteger(0)
    private val workerThreadDispatcher = Executors.newSingleThreadExecutor().asCoroutineDispatcher()
    private val threadPoolDispatcher =
        Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors())
            .asCoroutineDispatcher()
    private val byPassPatterns = mutableListOf<Regex>()
    private val dirCacheList = mutableListOf<WebDavDirNode>()

    override fun onStartJob(params: JobParameters?): Boolean {
        Log.i("ScanWebDavService", "Job start")
        if (!isNetworkAvailable()) {
            Log.w("ScanWebDavService", "Network not available")
            stopSelf()
        }
        val id = params?.extras?.getInt("id", -1)!!
        if (id == -1) {
            Log.e("ScanWebDavService", "id not provided")
            stopSelf()
        }

        CoroutineScope(workerThreadDispatcher).launch {
            val data = webDavRepository.getEntryById(id)
            data.onNone {
                Log.w("ScanWebDavService", "id not valid")
                stopSelf()
            }.onSome {
                baseUrl = it.url
            }

            Log.d("ScanWebDavService", "$data")
            execute(data)
        }
        return true
    }

    override fun onStopJob(params: JobParameters?): Boolean {
        Log.i("ScanWebDavService", "Job stop")
        return false
    }

    private suspend fun execute(data: Option<WebDavModel>) = option {
        val webDavData = data.bind()
        if (!checkIsWebDavDomainAvailable(
                webDavData.url,
                webDavData.loginId,
                webDavData.password
            )
        )
            throw Err("'${webDavData.url}' not reachable")

        Log.d("ScanWebDavService", "Start handling ${webDavData.url}")

        webDavData.bypassPattern
            .map {
                if (it.isRegex) Regex(it.pattern)
                else Regex(".*${it.pattern}.*")
            }
            .let {
                byPassPatterns.addAll(it)
                byPassPatterns.add(Regex(BOOK_METADATA_CONFIG_FILENAME))
            }

        val dirScanJob = getDirScanJob()
        val bookMarshalJob = getBookMarshalJob()
        dirScanJob.start()
        bookMarshalJob.start()

        pendingCheckingQueue.send(
            GetCheckListParam(
                webDavData.url,
                webDavData.loginId,
                webDavData.password
            )
        )

        CoroutineScope(Dispatchers.IO).launch {
            var fileCountSnapshot = 0
            var sameFileCountCounter = 0
            while (true) {
                val countNow = fileCount.get()
                val doneFileCountNow = doneFileCount.get()
                if (sameFileCountCounter >= 10 && doneFileCountNow == countNow) break

                Log.d(
                    "ScanWebDavService",
                    "File count now: $countNow, done file count now: $doneFileCountNow"
                )
                if (countNow == fileCountSnapshot) {
                    sameFileCountCounter++
                } else {
                    sameFileCountCounter = 0
                    fileCountSnapshot = countNow
                }
                delay(1000) // check it every one second
            }
            Log.d(
                "ScanWebDavService",
                "file count $fileCountSnapshot stable now, task can be finished"
            )
            dirScanJob.cancel("Done")
            bookMarshalJob.cancel("Done")
            writeDataToWebDav(
                Json.encodeToString(WebDavCacheData(dirCacheList,bookMetaDataLs)),
                BOOK_METADATA_CONFIG_FILENAME,
                webDavData.url,
                webDavData.loginId,
                webDavData.password
            )
            stopSelf()
            MainActivity.letScreenRest()
        }
    }.onNone {
        throw Err("id ${data.map { it.id }.getOrElse { -1 }} is not valid")
    }

    private suspend fun updateCheckingList(param: GetCheckListParam) {
        Log.d("updateCheckingList", "checking '${URLDecoder.decode(param.url, "UTF-8")}'")
        var ls = listOf<DirectoryViewModel.ContentData>()
        param.getList { ls = it }

        val currentPath = param.url.replace(baseUrl, "").split("/")
        val dirCache = WebDavDirNode(
            URLDecoder.decode(currentPath.last().ifEmpty { "/" }, "UTF-8"),
            if (currentPath.size == 1) null
            else if (currentPath.size == 2) "/"
            else URLDecoder.decode(currentPath.subList(0, currentPath.size - 1).joinToString("/"), "UTF-8"),
            ls.filter { it.isDir }.map { URLDecoder.decode(it.fullUrl.split("/").last(), "UTF-8") }
        )
        dirCacheList.add(dirCache)

        ls = ls.filter { item ->
            byPassPatterns.all {
                !it.matches(URLDecoder.decode(item.fullUrl, "UTF-8"))
            }
        }
        val folders = ls.filter { it.isDir }
        CoroutineScope(threadPoolDispatcher).launch {
            folders
                .forEach {
                    pendingCheckingQueue.send(
                        GetCheckListParam(
                            it.fullUrl,
                            param.loginId,
                            param.password
                        )
                    )
                }
        }
        val files = ls.filter { !it.isDir }
        CoroutineScope(threadPoolDispatcher).launch {
            files
                .forEach {
                    fileCount.addAndGet(1)
                    fileListQueue.send(it)
                }
        }

        Log.d(
            "getCheckingList",
            "'${
                URLDecoder.decode(
                    param.url,
                    "UTF-8"
                )
            }' has ${folders.size} folders and ${files.size} files"
        )
    }

    private fun getDirScanJob(): Deferred<Unit> {
        return CoroutineScope(threadPoolDispatcher).async {
            while (true) {
                val dirUrl = pendingCheckingQueue.receive()
                Log.d("ScanWebDavService", "Received url ${dirUrl.url} from queue")
                updateCheckingList(dirUrl)
            }
        }
    }

    private fun getBookMarshalJob(): Deferred<Unit> {
        return CoroutineScope(threadPoolDispatcher).async {
            while (true) {
                val book = fileListQueue.receive()
                Log.d("ScanWebDavService", "Received book ${book.name} from queue")
                bookMetaDataLsMutex.withLock {
                    bookMetaDataLs.add(BookMetaData(
                        name = book.name,
                        fileType = book.contentType?.run { "$type/$subtype" }
                            ?: BookMetaData.NOT_AVAILABLE,
                        relativePath = book.fullUrl
                    ))
                    doneFileCount.addAndGet(1)
                }
            }
        }
    }
}