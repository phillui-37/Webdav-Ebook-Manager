package xyz.kgy_production.webdavebookmanager.data.localdb.dao

import androidx.room.Dao
import androidx.room.Fts4
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Upsert
import xyz.kgy_production.webdavebookmanager.data.localdb.entity.TagsEntity

@Fts4
@Dao
interface TagsDAO {
    @Query("SELECT * FROM tags ORDER BY tag")
    suspend fun getAllOrderByTag()

    @Upsert
    suspend fun upsert(data: TagsEntity)

    @Insert
    suspend fun insert(data: TagsEntity)

    @Query("DELETE FROM tags WHERE tag = :tag")
    suspend fun deleteByTag(tag: String)
}