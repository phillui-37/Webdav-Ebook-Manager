package xyz.kgy_production.webdavebookmanager.data.localdb.dao

import androidx.room.Dao
import androidx.room.Fts4
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Upsert
import xyz.kgy_production.webdavebookmanager.data.localdb.entity.BookBookmarkEntity

@Fts4
@Dao
interface BookBookmarkDAO {
    @Query("SELECT * FROM book_bookmark WHERE book_id = :id")
    suspend fun getAllByBookId(id: Int): List<BookBookmarkEntity>

    @Upsert
    suspend fun upsert(data: BookBookmarkEntity)

    @Insert
    suspend fun insert(data: BookBookmarkEntity)

    @Query("DELETE FROM book_bookmark WHERE id = :id")
    suspend fun deleteById(id: Int)

    @Query("DELETE FROM book_bookmark WHERE book_id = :id")
    suspend fun deleteByBookId(id: Int)
}