package xyz.kgy_production.webdavebookmanager.data.localdb.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey

@Entity(
    tableName = "dir_tree_cache",
    foreignKeys = [ForeignKey(
        entity = WebDavEntity::class,
        parentColumns = arrayOf("id"),
        childColumns = arrayOf("web_dav_id"),
        onDelete = ForeignKey.CASCADE,
    )]
)
data class DirTreeCacheEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    @ColumnInfo(name = "web_dav_id", index = true) val webDavId: Int = 0,
    @ColumnInfo(name = "json_content") val jsonContent: String,
    @ColumnInfo(name = "last_update") val lastUpdated: String,
)