package xyz.kgy_production.webdavebookmanager.data.localdb.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey

@Entity(
    tableName = "book_bookmark",
    foreignKeys = [ForeignKey(
        entity = BookCacheEntity::class,
        parentColumns = arrayOf("id"),
        childColumns = arrayOf("book_id"),
        onDelete = ForeignKey.CASCADE
    )]
)
data class BookBookmarkEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    @ColumnInfo(name = "book_id") val bookId: Int,
    @ColumnInfo(name = "location") val location: Double,
    @ColumnInfo(name = "memo") val memo: String = ""
)
