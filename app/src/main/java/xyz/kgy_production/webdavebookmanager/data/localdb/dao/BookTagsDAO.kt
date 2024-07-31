package xyz.kgy_production.webdavebookmanager.data.localdb.dao

import androidx.room.Dao
import androidx.room.Fts4
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Upsert
import xyz.kgy_production.webdavebookmanager.data.localdb.entity.BookCacheEntity
import xyz.kgy_production.webdavebookmanager.data.localdb.entity.BookTagsEntity

@Fts4
@Dao
interface BookTagsDAO {
    @Query("SELECT b.* FROM book_tags bt, book_cache b WHERE bt.tag = :tag AND bt.book_id = b.id")
    suspend fun getAllBooksByTag(tag: String): List<BookCacheEntity>

    @Query("SELECT bt.tag FROM book_tags bt WHERE bt.book_id = :bookId")
    suspend fun getAllTagsByBookId(bookId: Int): List<String>

    @Query("SELECT DISTINCT bt.tag FROM book_tags bt ORDER BY bt.tag")
    suspend fun getAllTags(): List<String>

    @Upsert
    suspend fun upsert(data: BookTagsEntity)

    @Insert
    suspend fun insert(data: BookTagsEntity)

    @Query("DELETE FROM book_tags WHERE book_id = :id")
    suspend fun deleteByBookId(id: Int)

    @Query("DELETE FROM book_tags WHERE book_id = :bookId AND tag = :tag")
    suspend fun deleteByBookIdAndTag(bookId: Int, tag: String)
}