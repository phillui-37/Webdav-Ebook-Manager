package xyz.kgy_production.webdavebookmanager

import android.app.Application
import android.content.Context
import androidx.datastore.preferences.preferencesDataStore
import arrow.core.Some
import arrow.core.toOption
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import xyz.kgy_production.webdavebookmanager.data.WebDavRepository
import javax.inject.Inject

@HiltAndroidApp
class MainApplication : Application() {
    @Inject lateinit var webDavRepository: WebDavRepository

    companion object {
        val Context.dataStore by preferencesDataStore(name = "config")
    }

    override fun onCreate() {
        super.onCreate()

        createDevContent()
    }

    private fun createDevContent() {
        val url = resources.getString(R.string.webdavUrl)
        val loginId = resources.getString(R.string.webdavLoginId)
        val password = resources.getString(R.string.webdavLoginPassword)

        if (arrayOf(url, loginId, password).all { it.isNotEmpty() }) {
            CoroutineScope(Dispatchers.IO).launch {
                if (webDavRepository.getEntryByUrlAndLoginId(url, loginId).isNone()) {
                    webDavRepository.createEntry(
                        "Dev Server".toOption(),
                        url,
                        loginId.toOption(),
                        password.toOption()
                    )
                }
            }
        }
    }
}