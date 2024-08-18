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
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.delay
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
    private val pendingCheckingList = mutableListOf<GetCheckListParam>()
    private val doneCheckingList = mutableListOf<String>()
    private val bookMetaDataLs = mutableListOf<BookMetaData>()
    private val workerThreadDispatcher = Executors.newSingleThreadExecutor().asCoroutineDispatcher()
    private val pendingMutex = Mutex()
    private val dirMutex = Mutex()
    private val bookMutex = Mutex()
    private val byPassPatterns = mutableListOf<Regex>()
    private val dirCacheList = mutableListOf<WebDavDirNode>()

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
            notify(
                NotificationChannelEnum.ScanWebDavService.tag,
                NotificationChannelEnum.ScanWebDavService.id,
                noti
            )
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
            .filter {
                if (it.packageName != packageName) {
                    false
                } else {
                    it.id == NotificationChannelEnum.ScanWebDavService.id
                            && it.tag == NotificationChannelEnum.ScanWebDavService.tag
                }
            }
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
                byPassPatterns.add(Regex(".*${BOOK_METADATA_CONFIG_FILENAME}.*"))
            }

        pendingCheckingList.add(
            GetCheckListParam(
                webDavData.url,
                webDavData.loginId,
                webDavData.password
            )
        )

        CoroutineScope(Dispatchers.IO).launch {
            var isNotEmpty = pendingMutex.withLock { pendingCheckingList.size != 0 }
            var firstTime = true
            while (isNotEmpty) {
                checkAndRecoverNoti()
                logger.d(
                    "File count now: ${bookMetaDataLs.size}, pending list size: ${pendingCheckingList.size}"
                )
                try {
                    val param = pendingMutex.withLock {
                        pendingCheckingList.removeFirst()
                    }
                    CoroutineScope(Dispatchers.IO).launch {
                        updateCheckingList(param)
                    }
                    if (firstTime) {
                        delay(2000)
                        firstTime = false
                    }
                } catch (e: NoSuchElementException) {
                    continue
                } finally {
                    delay(500)
                    isNotEmpty = pendingMutex.withLock { pendingCheckingList.size != 0 }
                }
            }
            logger.d("task done")

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
        dirMutex.withLock { dirCacheList.add(dirCache) }

        ls = pendingMutex.withLock {
            ls.filter { item ->
                byPassPatterns.all {
                    !it.matches(item.fullUrl.urlDecode()) && !it.matches(item.name)
                } && !doneCheckingList.contains(item.fullUrl)
            }
        }

        val folders = ls.filter { it.isDir }
        logger.d("push ${folders.size} dirs to queue")
        pendingMutex.withLock {
            folders
                .map {
                    doneCheckingList.add(it.fullUrl)
                    GetCheckListParam(
                        it.fullUrl,
                        param.loginId,
                        param.password
                    )
                }
                .also(pendingCheckingList::addAll)
        }

        val files = bookMutex.withLock {
            ls.filter {
                !it.isDir && bookMetaDataLs.find { book -> it.name == book.name && it.fullUrl == book.fullUrl } == null
            }
        }
        logger.d("push ${files.size} files to queue")
        marshalBook(baseUrl, files)

        logger.d(
            "[getCheckingList] '${
                param.url.urlDecode()
            }' has ${folders.size} folders and ${files.size} files"
        )
    }

    private suspend fun marshalBook(baseUrl: String, books: List<DirectoryViewModel.ContentData>) {
        logger.d("execute getBookMarshalJob")
        books.forEach { book ->
            logger.d("Received book ${book.name} from queue")
            val bookRelativePath = book.fullUrl.replace(baseUrl, "")
            val dupBook = bookMutex.withLock {
                bookMetaDataLs.find { it.name == book.name && it.relativePath == bookRelativePath }
            }
            if (dupBook == null) {
                bookMutex.withLock {
                    bookMetaDataLs.add(BookMetaData(
                        name = book.name,
                        fileType = book.contentType?.run { "$type/$subtype" }
                            ?: BookMetaData.NOT_AVAILABLE,
                        fullUrl = book.fullUrl,
                        relativePath = book.fullUrl.replace(baseUrl, ""),
                        lastUpdated = LocalDateTime.now()
                    ))
                }
            } else {
                logger.d("duplicate book:\nreceived->$book\nexists->$dupBook$")
            }
        }
    }

    private fun tidyUp(
        reason: String,
        cancelNotiAction: () -> Unit
    ) {
        cancelNotiAction()
        stopSelf()
        MainActivity.letScreenRest()
    }
}