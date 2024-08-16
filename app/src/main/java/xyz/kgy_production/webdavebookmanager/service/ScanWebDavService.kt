package xyz.kgy_production.webdavebookmanager.service

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.job.JobInfo
import android.app.job.JobParameters
import android.app.job.JobScheduler
import android.app.job.JobService
import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.PersistableBundle
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import xyz.kgy_production.webdavebookmanager.MainActivity
import xyz.kgy_production.webdavebookmanager.R
import xyz.kgy_production.webdavebookmanager.data.model.BookMetaData
import xyz.kgy_production.webdavebookmanager.data.model.WebDavCacheData
import xyz.kgy_production.webdavebookmanager.data.model.WebDavDirNode
import xyz.kgy_production.webdavebookmanager.data.model.WebDavModel
import xyz.kgy_production.webdavebookmanager.data.repository.WebDavRepository
import xyz.kgy_production.webdavebookmanager.ui.viewmodel.DirectoryViewModel
import xyz.kgy_production.webdavebookmanager.util.BOOK_METADATA_CONFIG_FILENAME
import xyz.kgy_production.webdavebookmanager.util.Logger
import xyz.kgy_production.webdavebookmanager.util.NotificationChannelEnum
import xyz.kgy_production.webdavebookmanager.util.checkIsWebDavDomainAvailable
import xyz.kgy_production.webdavebookmanager.util.getWebDavDirContentList
import xyz.kgy_production.webdavebookmanager.util.isNetworkAvailable
import xyz.kgy_production.webdavebookmanager.util.urlDecode
import xyz.kgy_production.webdavebookmanager.util.writeDataToWebDav
import java.time.LocalDateTime
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
import javax.inject.Inject

@AndroidEntryPoint
class ScanWebDavService : JobService() {

    companion object {
        private val logger by Logger.delegate(ScanWebDavService::class.java)

        fun startScanService(ctx: Context, id: Int, startNow: Boolean = true) {
            logger.d("add service task to scheduler for model id: $id")
            val scheduler = ctx.getSystemService(JobScheduler::class.java)
            val builder = JobInfo.Builder(
                NotificationChannelEnum.ScanWebDavService.id,
                ComponentName(ctx, ScanWebDavService::class.java)
            ).apply {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    setMinimumLatency(0L)
                }
                if (!startNow) {
                    setRequiredNetworkType(JobInfo.NETWORK_TYPE_UNMETERED)
                    setRequiresBatteryNotLow(true)
                }
            }

            val bundle = PersistableBundle()
            bundle.putInt("id", id)
            builder.setExtras(bundle)

            scheduler.schedule(builder.build())
        }
    }

    @Inject
    lateinit var webDavRepository: WebDavRepository

    private class Err(msg: String) : RuntimeException("ScanWebDavService: $msg")

    private var noti: Notification? = null

    private data class GetCheckListParam(
        val url: String,
        val loginId: String,
        val password: String,
    ) {
        suspend fun getList(setter: (List<DirectoryViewModel.ContentData>) -> Unit) {
            getWebDavDirContentList(url, loginId, password, setter)
        }
    }

    private lateinit var baseUrl: String
    private val pendingCheckingQueue = MutableSharedFlow<GetCheckListParam>()
    private val fileListQueue = MutableSharedFlow<DirectoryViewModel.ContentData>()
    private val bookMetaDataLs = mutableListOf<BookMetaData>()
    private val bookMetaDataLsMutex = Mutex()
    private val receiveFileCount = AtomicInteger(0)
    private val doneFileCount = AtomicInteger(0)
    private val workerThreadDispatcher = Executors.newSingleThreadExecutor().asCoroutineDispatcher()
    private val dirDispatcher =
        Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors() / 2)
            .asCoroutineDispatcher()
    private val bookDispatcher =
        Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors() / 2)
            .asCoroutineDispatcher()
    private val byPassPatterns = mutableListOf<Regex>()
    private val dirCacheList = mutableListOf<WebDavDirNode>()
    private var isNotDone = AtomicBoolean(true)
    private var dirTask: Job? = null
    private var fileTask: Job? = null

    override fun onStartJob(params: JobParameters?): Boolean {
        logger.i("Job start")
        if (!isNetworkAvailable()) {
            logger.w("Network not available")
            stopSelf()
        }
        val id = params?.extras?.getInt("id", -1)!!
        if (id == -1) {
            logger.e("id not provided")
            tidyUp("id not provided") {}
        }

        CoroutineScope(workerThreadDispatcher).launch {
            val data = webDavRepository.getEntryById(id)
            data?.let {
                baseUrl = it.url
            } ?: run {
                logger.w("id not valid")
                tidyUp("id not valid") {}
            }

            logger.d("$data")
            try {
                execute(data)
            } catch (e: Exception) {
                e.printStackTrace()
                tidyUp("fail") {
                    NotificationManagerCompat.from(this@ScanWebDavService)
                        .cancel(NotificationChannelEnum.ScanWebDavService.id)
                }
            }
        }
        return true
    }

    override fun onStopJob(params: JobParameters?): Boolean {
        logger.i("Job stop")
        return false
    }

    private fun createNotificationChannel() {
        val name = NotificationChannelEnum.ScanWebDavService.tag
        val descriptionText = "Background webdav dir scan service notification"
        val importance = NotificationManager.IMPORTANCE_LOW
        val channel = NotificationChannel(
            name,
            name,
            importance
        ).apply {
            description = descriptionText
        }
        // Register the channel with the system.
        val notificationManager = getSystemService(NotificationManager::class.java)
        notificationManager.createNotificationChannel(channel)
    }

    private fun postNoti(noti: Notification) {
        with(NotificationManagerCompat.from(this)) {
            if (ActivityCompat.checkSelfPermission(
                    this@ScanWebDavService,
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                return@with
            }
            notify(NotificationChannelEnum.ScanWebDavService.id, noti)
        }
    }

    private fun sendTaskNotification(dirName: String): () -> Unit {
        createNotificationChannel()
        val builder =
            NotificationCompat.Builder(this, NotificationChannelEnum.ScanWebDavService.tag)
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .setAutoCancel(false)
                .setContentTitle(resources.getString(R.string.app_name))
                .setContentText("Scanning $dirName")
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .setOngoing(true)
        noti = builder.build()
        postNoti(noti!!)
        return {
            NotificationManagerCompat.from(this)
                .cancel(NotificationChannelEnum.ScanWebDavService.id)
        }
    }

    private fun checkAndRecoverNoti() {
        getSystemService(NotificationManager::class.java)
            .activeNotifications
            .filter { it.id == NotificationChannelEnum.ScanWebDavService.id && it.tag == NotificationChannelEnum.ScanWebDavService.tag }
            .ifEmpty {
                postNoti(noti!!)
            }
    }

    private suspend fun execute(data: WebDavModel?) = data?.let { webDavData ->
        if (!checkIsWebDavDomainAvailable(
                webDavData.url,
                webDavData.loginId,
                webDavData.password
            )
        )
            throw Err("'${webDavData.url}' not reachable")

        dirTask = getDirScanJob().apply { start() }
        fileTask = getBookMarshalJob(webDavData.url).apply { start() }

        MainActivity.keepScreenOn()
        logger.d("Start handling ${webDavData.url.urlDecode()}")
        val cancelNotiAction =
            sendTaskNotification(webDavData.name.ifEmpty { webDavData.url.urlDecode() })

        webDavData.bypassPattern
            .map {
                if (it.isRegex) Regex(it.pattern)
                else Regex(".*${it.pattern}.*")
            }
            .let {
                byPassPatterns.addAll(it)
                byPassPatterns.add(Regex(BOOK_METADATA_CONFIG_FILENAME))
            }

        pendingCheckingQueue.emit(
            GetCheckListParam(
                webDavData.url,
                webDavData.loginId,
                webDavData.password
            )
        )

        CoroutineScope(Dispatchers.IO).launch {
            var fileCountSnapshot = 0
            var sameFileCountCounter = 0
            while (isNotDone.get()) {
                checkAndRecoverNoti()
                val doneFileCountNow = doneFileCount.get()
                if (sameFileCountCounter >= 10 && doneFileCountNow == fileCountSnapshot)
                    isNotDone.set(true)
                else {
                    logger.d(
                        "File count now: $doneFileCountNow, file count before: $fileCountSnapshot, received file: ${receiveFileCount.get()}"
                    )
                    if (doneFileCountNow == fileCountSnapshot) {
                        sameFileCountCounter++
                    } else {
                        sameFileCountCounter = 0
                        fileCountSnapshot = doneFileCountNow
                    }
                    delay(1000) // check it every one second
                }
            }
            isNotDone.set(true)
            logger.d(
                "file count $fileCountSnapshot stable now, task can be finished"
            )

            // save the conf
            writeDataToWebDav(
                Json.encodeToString(WebDavCacheData(dirCacheList, bookMetaDataLs).sorted()),
                BOOK_METADATA_CONFIG_FILENAME,
                webDavData.url,
                webDavData.loginId,
                webDavData.password
            )
//            this@ScanWebDavService.saveWebDavCache(
//                WebDavCacheData(dirCacheList, bookMetaDataLs).sorted(),
//                webDavData.uuid
//            )

            tidyUp("Done", cancelNotiAction)
        }
    } ?: throw Err("null data is provided")

    private suspend fun updateCheckingList(param: GetCheckListParam) {
        logger.d("[updateCheckingList] checking '${param.url.urlDecode()}'")
        var ls = listOf<DirectoryViewModel.ContentData>()
        try {
            param.getList { ls = it }
        } catch (e: Exception) {
            e.printStackTrace()
            tidyUp("fail") {
                NotificationManagerCompat.from(this)
                    .cancel(NotificationChannelEnum.ScanWebDavService.id)
            }
        }

        val currentPath = param.url.replace(baseUrl, "").split("/")
        val dirCache = WebDavDirNode(
            currentPath.last().ifEmpty { "/" },
            when (currentPath.size) {
                1 -> null
                2 -> "/"
                else -> currentPath.subList(0, currentPath.size - 1).joinToString("/")
            },
            ls.map { it.fullUrl.split("/").last() },
            LocalDateTime.now(),
        )
        dirCacheList.add(dirCache)

        ls = ls.filter { item ->
            byPassPatterns.all {
                !it.matches(item.fullUrl.urlDecode())
            }
        }

        val folders = ls.filter { it.isDir }
        logger.d("push ${folders.size} dirs to queue")
        CoroutineScope(Dispatchers.IO).launch {
            folders.forEach {
                pendingCheckingQueue.emit(
                    GetCheckListParam(
                        it.fullUrl,
                        param.loginId,
                        param.password
                    )
                )
            }
        }

        val files = bookMetaDataLsMutex.withLock {
            ls.filter {
                !it.isDir && bookMetaDataLs.find { book -> it.name == book.name && it.fullUrl == book.fullUrl } == null
            }
        }
        logger.d("push ${files.size} files to queue")
        CoroutineScope(Dispatchers.IO).launch {
            files.forEach {
                fileListQueue.emit(it)
            }
        }

        logger.d(
            "[getCheckingList] '${
                param.url.urlDecode()
            }' has ${folders.size} folders and ${files.size} files"
        )
    }

    private fun getDirScanJob(): Job {
        return CoroutineScope(dirDispatcher).launch {
            logger.d("execute getDirScanJob")
            pendingCheckingQueue.collect { dirUrl ->
                logger.d("Received url ${dirUrl.url} from queue")
                CoroutineScope(dirDispatcher).launch {
                    updateCheckingList(dirUrl)
                }
            }
        }
    }

    private fun getBookMarshalJob(baseUrl: String): Job {
        return CoroutineScope(bookDispatcher).launch {
            logger.d("execute getBookMarshalJob")
            fileListQueue.collect { book ->
                logger.d("Received book ${book.name} from queue")
                bookMetaDataLsMutex.withLock {
                    receiveFileCount.addAndGet(1)
                    val bookRelativePath = book.fullUrl.replace(baseUrl, "")
                    val dupBook =
                        bookMetaDataLs.find { it.name == book.name && it.relativePath == bookRelativePath }
                    if (dupBook == null) {
                        bookMetaDataLs.add(BookMetaData(
                            name = book.name,
                            fileType = book.contentType?.run { "$type/$subtype" }
                                ?: BookMetaData.NOT_AVAILABLE,
                            fullUrl = book.fullUrl,
                            relativePath = book.fullUrl.replace(baseUrl, ""),
                            lastUpdated = LocalDateTime.now()
                        ))
                        doneFileCount.addAndGet(1)
                    } else {
                        logger.d("duplicate book:\nreceived->$book\nexists->$dupBook$")
                    }
                }
            }
        }
    }

    private fun tidyUp(
        reason: String,
        cancelNotiAction: () -> Unit
    ) {
        dirTask?.cancel(reason)
        fileTask?.cancel(reason)
        cancelNotiAction()
        bookDispatcher.close()
        dirDispatcher.close()
        stopSelf()
        MainActivity.letScreenRest()
    }
}