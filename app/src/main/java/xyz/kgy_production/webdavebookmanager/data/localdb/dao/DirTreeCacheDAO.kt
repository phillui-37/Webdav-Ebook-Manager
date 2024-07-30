package xyz.kgy_production.webdavebookmanager.data.localdb.dao

import androidx.room.Dao
import androidx.room.Fts4
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Upsert
import xyz.kgy_production.webdavebookmanager.data.localdb.entity.DirTreeCacheEntity

@Fts4
@Dao
interface DirTreeCacheDAO {
    @Query("SELECT * FROM dir_tree_cache WHERE web_dav_id = :id")
    suspend fun getById(id: Int): DirTreeCacheEntity?

    @Upsert
    suspend fun upsert(data: DirTreeCacheEntity)

    @Insert
    suspend fun insert(data: DirTreeCacheEntity)

    @Query("DELETE FROM dir_tree_cache WHERE id = :id")
    suspend fun deleteById(id: Int)
}