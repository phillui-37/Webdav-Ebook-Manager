package xyz.kgy_production.webdavebookmanager.data.localdb

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [WebDavEntity::class],
    version = 1,
    exportSchema = true,
)
abstract class WebDavDatabase : RoomDatabase() {
    abstract fun webDavDao(): WebDavDAO
}