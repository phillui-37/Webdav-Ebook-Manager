package xyz.kgy_production.webdavebookmanager

import android.app.Application
import android.content.Context
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class MainApplication: Application() {
    companion object {
        val Context.dataStore by preferencesDataStore(name = "config")
    }
}