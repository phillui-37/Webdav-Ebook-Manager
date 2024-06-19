package xyz.kgy_production.webdavebookmanager.data.localdb

import androidx.room.Dao
import androidx.room.Fts4
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Upsert

@Fts4
@Dao
interface WebDavDAO {
    @Query("SELECT * from webdav")
    suspend fun getAll(): List<WebDavEntity>

    @Query("SELECT * from webdav where rowid = :id")
    suspend fun getById(id: Int): WebDavEntity?

    @Query("select * from webdav where uuid = :uuid")
    suspend fun getByUuid(uuid: String): WebDavEntity?

    @Insert
    suspend fun insert(entry: WebDavEntity)

    @Upsert
    suspend fun upsert(entry: WebDavEntity)

    @Query("update webdav set is_active = :isActive where rowid = :id")
    suspend fun setIsActive(id: Int, isActive: Boolean)

    @Query("DELETE FROM webdav where rowid = :id")
    suspend fun deleteById(id: Int)
}