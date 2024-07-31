package xyz.kgy_production.webdavebookmanager.data.localdb

import androidx.room.Database
import androidx.room.RoomDatabase
import xyz.kgy_production.webdavebookmanager.data.localdb.dao.BookBookmarkDAO
import xyz.kgy_production.webdavebookmanager.data.localdb.dao.BookCacheDAO
import xyz.kgy_production.webdavebookmanager.data.localdb.dao.BookTagsDAO
import xyz.kgy_production.webdavebookmanager.data.localdb.dao.DirTreeCacheDAO
import xyz.kgy_production.webdavebookmanager.data.localdb.dao.WebDavDAO
import xyz.kgy_production.webdavebookmanager.data.localdb.entity.BookBookmarkEntity
import xyz.kgy_production.webdavebookmanager.data.localdb.entity.BookCacheEntity
import xyz.kgy_production.webdavebookmanager.data.localdb.entity.BookTagsEntity
import xyz.kgy_production.webdavebookmanager.data.localdb.entity.DirTreeCacheEntity
import xyz.kgy_production.webdavebookmanager.data.localdb.entity.WebDavEntity

@Database(
    entities = [
        WebDavEntity::class,
        DirTreeCacheEntity::class,
        BookCacheEntity::class,
        BookBookmarkEntity::class,
        BookTagsEntity::class,
    ],
    version = 1,
    exportSchema = true,
)
abstract class WebDavDatabase : RoomDatabase() {
    abstract fun webDavDao(): WebDavDAO
    abstract fun dirTreeDao(): DirTreeCacheDAO
    abstract fun bookCacheDao(): BookCacheDAO
    abstract fun bookBookmarkDao(): BookBookmarkDAO
    abstract fun bookTagsDao(): BookTagsDAO
}