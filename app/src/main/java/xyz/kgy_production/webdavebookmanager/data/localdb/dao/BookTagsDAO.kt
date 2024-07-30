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
    @Query("SELECT b.* FROM tags t, book_tags bt, book_cache b WHERE t.tag = :tag AND bt.tag_id = t.id AND bt.book_id = b.id")
    suspend fun getAllBooksByTag(tag: String): List<BookCacheEntity>

    @Query("SELECT t.tag FROM tags t, book_tags bt WHERE bt.book_id = :bookId AND bt.tag_id = t.id")
    suspend fun getAllTagsByBookId(bookId: Int): List<String>

    @Upsert
    suspend fun upsert(data: BookTagsEntity)

    @Insert
    suspend fun insert(data: BookTagsEntity)

    @Query("DELETE FROM book_tags WHERE book_id = :id")
    suspend fun deleteByBookId(id: Int)

    @Query("DELETE FROM book_tags WHERE book_id = :bookId AND tag_id = :tagId")
    suspend fun deleteByBookIdAndTag(bookId: Int, tagId: Int)
}