package xyz.kgy_production.webdavebookmanager.data.localdb.dao

import androidx.room.Dao
import androidx.room.Fts4
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Upsert
import xyz.kgy_production.webdavebookmanager.data.localdb.entity.BookCacheEntity

@Fts4
@Dao
interface BookCacheDAO {
    @Query("SELECT * FROM book_cache WHERE web_dav_id = :id")
    suspend fun getAllByWebDavId(id: Int): List<BookCacheEntity>

    @Query("SELECT * FROM book_cache WHERE web_dav_id = :id AND author LIKE '%' || :author || '%'")
    suspend fun searchAllByWebDavIdAndAuthor(id: Int, author: String): List<BookCacheEntity>

    @Query("SELECT * FROM book_cache WHERE web_dav_id = :id AND publisher LIKE '%' || :publisher || '%'")
    suspend fun searchAllByWebDavIdAndPublisher(id: Int, publisher: String): List<BookCacheEntity>

    @Query("SELECT * FROM book_cache WHERE web_dav_id = :id AND series = :series ORDER BY `order`")
    suspend fun getAllByWebDavIdAndSeriesOrderByOrder(
        id: Int,
        series: String
    ): List<BookCacheEntity>

    @Query("SELECT * FROM book_cache WHERE web_dav_id = :id AND url = :url")
    suspend fun getByWebDavIdAndUrl(id: Int, url: String): BookCacheEntity?

    @Query("SELECT * FROM book_cache WHERE id = :id")
    suspend fun getById(id: Int)

    @Query("SELECT * FROM book_cache WHERE read_progress = 0.0 AND is_read = 0")
    suspend fun getAllUnread()

    @Query("SELECT * FROM book_cache WHERE read_progress > 0.0 AND is_read = 0")
    suspend fun getAllReading()

    @Query("SELECT * FROM book_cache WHERE is_read = 1")
    suspend fun getAllRead()

    @Insert
    suspend fun insert(data: BookCacheEntity)

    @Upsert
    suspend fun upsert(data: BookCacheEntity)

    @Query("DELETE FROM book_cache WHERE id = :id")
    suspend fun deleteById(id: Int)
}