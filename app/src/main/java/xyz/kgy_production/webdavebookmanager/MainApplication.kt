package xyz.kgy_production.webdavebookmanager

import android.app.Application
import android.content.Context
import android.webkit.WebView
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import xyz.kgy_production.webdavebookmanager.data.repository.WebDavRepository
import xyz.kgy_production.webdavebookmanager.util.Logger
import xyz.kgy_production.webdavebookmanager.util.urlEncode
import javax.inject.Inject

@HiltAndroidApp
class MainApplication : Application() {
    @Inject lateinit var webDavRepository: WebDavRepository
    private val logger by Logger.delegate(this::class.java)

    companion object {
        val Context.dataStore by preferencesDataStore(name = "config")
    }

    override fun onCreate() {
        super.onCreate()
        createDevContent()
        WebView.setWebContentsDebuggingEnabled(BuildConfig.DEBUG)
    }

    private fun createDevContent() {
        val url = resources.getString(R.string.webdavUrl)
        val loginId = resources.getString(R.string.webdavLoginId)
        val password = resources.getString(R.string.webdavLoginPassword)
        val encodedUrl = url.urlEncode()

        if (arrayOf(url, loginId, password).all { it.isNotEmpty() }) {
            logger.d("found testing webdav entry, url:$url, loginId:$loginId")
            CoroutineScope(Dispatchers.IO).launch {
                if (webDavRepository.getEntryByUrlAndLoginId(encodedUrl, loginId) == null) {
                    logger.d("appending testing webdav entry")
                    webDavRepository.createEntry(
                        "Dev Server",
                        encodedUrl,
                        loginId,
                        password,
                        listOf(),
                        true
                    )
                }
            }
        }
    }
}