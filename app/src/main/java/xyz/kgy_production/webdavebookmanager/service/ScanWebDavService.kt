package xyz.kgy_production.webdavebookmanager.service

import android.app.ForegroundServiceStartNotAllowedException
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.graphics.Bitmap
import android.os.Build
import android.util.Log
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Book
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import androidx.core.graphics.drawable.IconCompat
import arrow.core.MemoizedDeepRecursiveFunction
import arrow.core.Option
import arrow.core.getOrElse
import arrow.core.raise.fold
import arrow.core.raise.option
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import xyz.kgy_production.webdavebookmanager.MainActivity
import xyz.kgy_production.webdavebookmanager.R
import xyz.kgy_production.webdavebookmanager.data.WebDavRepository
import xyz.kgy_production.webdavebookmanager.data.model.BookMetaData
import xyz.kgy_production.webdavebookmanager.data.model.WebDavModel
import xyz.kgy_production.webdavebookmanager.util.BOOK_METADATA_CONFIG_FILENAME
import xyz.kgy_production.webdavebookmanager.util.NotificationChannelEnum
import xyz.kgy_production.webdavebookmanager.util.checkIsWebDavDomainAvailable
import xyz.kgy_production.webdavebookmanager.util.getWebDavDirContentList
import xyz.kgy_production.webdavebookmanager.util.isNetworkAvailable
import xyz.kgy_production.webdavebookmanager.util.writeDataToWebDav
import xyz.kgy_production.webdavebookmanager.viewmodel.DirectoryViewModel
import java.net.URLDecoder
import javax.inject.Inject
import kotlin.math.log

@AndroidEntryPoint
class ScanWebDavService : Service() {
    @Inject
    lateinit var webDavRepository: WebDavRepository

    companion object {
        fun createNotiChannel(ctx: Context) {
            val ch = NotificationChannel(
                NotificationChannelEnum.ScanWebDavService.tag,
                NotificationChannelEnum.ScanWebDavService.tag,
                NotificationManager.IMPORTANCE_LOW
            )
            ch.lockscreenVisibility = Notification.VISIBILITY_PUBLIC
            ch.enableLights(false)
            ch.enableVibration(false)
            ch.setSound(null, null)
            val manager = ctx.getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(ch)
        }
    }

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

    private val contentMapCache = mutableMapOf<String, List<DirectoryViewModel.ContentData>>()

    override fun onBind(intent: Intent?) = null

    private fun startForeground() {
        try {
            val pIntent = PendingIntent.getActivity(
                this,
                NotificationChannelEnum.ScanWebDavService.id,
                Intent(this,MainActivity::class.java),
                PendingIntent.FLAG_UPDATE_CURRENT
            )
            val noti = NotificationCompat
                .Builder(this, NotificationChannelEnum.ScanWebDavService.tag)
                .setOngoing(true)
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .setContentTitle(getString(R.string.app_name))
                .setContentText(getString(R.string.noti_scan_webdav_title))
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .setChannelId(NotificationChannelEnum.ScanWebDavService.tag)
                .setContentIntent(pIntent)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                noti.setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
            }
            startForeground(
                NotificationChannelEnum.ScanWebDavService.id,
                noti.build(),
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC else 0
            )
//            ServiceCompat.startForeground(
//                this,
//                NotificationChannelEnum.ScanWebDavService.id,
//                noti.build(),
//                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC else 0
//            )
        } catch (e: Exception) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && e is ForegroundServiceStartNotAllowedException) {
                // TODO start from background
            }
        }
    }

    private fun execute(data: Option<WebDavModel>) = option {
        val webDavData = data.bind()
        if (!checkIsWebDavDomainAvailable(
                webDavData.url,
                webDavData.loginId,
                webDavData.password
            )
        )
            throw Err("'${webDavData.url}' not reachable")

        val bookMetaDataLs = mutableListOf<BookMetaData>()
        getCheckingList(GetCheckListParam(webDavData.url, webDavData.loginId, webDavData.password))
            .forEach { webdav ->
                contentMapCache[webdav.fullUrl]!!
                    .filter { !it.isDir }
                    .forEach { book ->

                        bookMetaDataLs.add(BookMetaData(
                            name = book.name,
                            fileType = book.contentType?.run { "$type/$subtype" }
                                ?: BookMetaData.NOT_AVAILABLE,
                            relativePath = book.fullUrl
                        ))
                    }
            }

        writeDataToWebDav(
            bookMetaDataLs,
            BOOK_METADATA_CONFIG_FILENAME,
            webDavData.url,
            webDavData.loginId,
            webDavData.password
        )

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R)
            stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }.onNone {
        throw Err("id ${data.map { it.id }.getOrElse { -1 }} is not valid")
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (!isNetworkAvailable()) {
            Log.w("ScanWebDavService", "Network not available")
            stopSelf()
        }
        val id = intent?.getIntExtra("id", -1)!!
        if (id == -1) {
            Log.e("ScanWebDavService", "id not provided")
            stopSelf()
        }

        startForeground()
        CoroutineScope(Dispatchers.IO).launch {
            val data = webDavRepository.getEntryById(id)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                data.onNone {
                    Log.w("ScanWebDavService", "id not valid")
                    stopSelf()
                }
            }
            execute(data)
        }

        return START_STICKY
    }

    private val getCheckingList = MemoizedDeepRecursiveFunction<GetCheckListParam, List<DirectoryViewModel.ContentData>> { param ->
        Log.d("getCheckingList", "checking '${URLDecoder.decode(param.url, "UTF-8")}'")
        if (contentMapCache.containsKey(param.url))
            contentMapCache[param.url]!!
        else {
            var ls = listOf<DirectoryViewModel.ContentData>()
            param.getList { ls = it }
            val folderCount = ls.count { it.isDir }
            val fileCount = ls.size - folderCount

            Log.d("getCheckingList", "'${URLDecoder.decode(param.url, "UTF-8")}' has $folderCount folders and $fileCount files")
            contentMapCache[param.url] = ls
            ls + ls.filter { it.isDir }
                .flatMap { callRecursive(param.copy(url = it.fullUrl)) }
        }
    }
}