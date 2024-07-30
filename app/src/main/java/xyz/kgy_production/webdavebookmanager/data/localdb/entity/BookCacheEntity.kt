package xyz.kgy_production.webdavebookmanager.data.localdb.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey

@Entity(
    tableName = "book_cache",
    foreignKeys = [ForeignKey(
        entity = WebDavEntity::class,
        parentColumns = arrayOf("id"),
        childColumns = arrayOf("web_dav_id"),
        onDelete = ForeignKey.CASCADE,
    )]
)
data class BookCacheEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    @ColumnInfo(name = "web_dav_id", index = true) val webDavId: Int = 0,
    @ColumnInfo(name = "name") val name: String,
    @ColumnInfo(name = "url") val url: String,
    @ColumnInfo(name = "local_path") val localPath: String,
    @ColumnInfo(name = "series") val series: String,
    @ColumnInfo(name = "order") val orderInSeries: Int,
    @ColumnInfo(name = "author") val author: String,
    @ColumnInfo(name = "publisher") val publisher: String,
    @ColumnInfo(name = "file_type") val fileType: String,
    @ColumnInfo(name = "is_read") val isRead: Boolean,
    @ColumnInfo(name = "read_progress") val readProgress: Double,
    @ColumnInfo(name = "last_update") val lastUpdate: String,
)
