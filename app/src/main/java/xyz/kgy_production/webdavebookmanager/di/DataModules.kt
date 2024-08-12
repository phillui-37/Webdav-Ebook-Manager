package xyz.kgy_production.webdavebookmanager.di

import android.content.Context
import androidx.room.Room
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import xyz.kgy_production.webdavebookmanager.data.localdb.WebDavDatabase
import xyz.kgy_production.webdavebookmanager.data.repository.DirTreeCacheRepository
import xyz.kgy_production.webdavebookmanager.data.repository.WebDavRepository
import xyz.kgy_production.webdavebookmanager.data.repository.impl.DefaultDirTreeCacheRepository
import xyz.kgy_production.webdavebookmanager.data.repository.impl.DefaultWebDavRepository
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DataBaseModule {
    @Singleton
    @Provides
    fun provideDatabase(@ApplicationContext ctx: Context): WebDavDatabase {
        return Room.databaseBuilder(
            ctx.applicationContext,
            WebDavDatabase::class.java,
            "webdav.db"
        ).build()
    }


    @Provides
    fun provideWebDavDao(db: WebDavDatabase) = db.webDavDao()
    @Provides
    fun provideDirTreeDao(db: WebDavDatabase) = db.dirTreeDao()
    @Provides
    fun provideBookCacheDao(db: WebDavDatabase) = db.bookCacheDao()
    @Provides
    fun provideBookBookmarkDao(db: WebDavDatabase) = db.bookBookmarkDao()
    @Provides
    fun provideBookTagsDao(db: WebDavDatabase) = db.bookTagsDao()
}

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {
    @Singleton
    @Binds
    abstract fun bindWebDavRepository(repository: DefaultWebDavRepository): WebDavRepository

    @Singleton
    @Binds
    abstract fun bindDirTreeRepository(repository: DefaultDirTreeCacheRepository): DirTreeCacheRepository
}