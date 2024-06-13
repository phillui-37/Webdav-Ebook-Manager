package xyz.kgy_production.webdavebookmanager.di

import android.content.Context
import androidx.room.Room
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import xyz.kgy_production.webdavebookmanager.data.DefaultWebDavRepository
import xyz.kgy_production.webdavebookmanager.data.WebDavRepository
import xyz.kgy_production.webdavebookmanager.data.localdb.WebDavDatabase
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
}

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {
    @Singleton
    @Binds
    abstract fun bindWebDavRepository(repository: DefaultWebDavRepository): WebDavRepository
}