package xyz.kgy_production.webdavebookmanager.data.localdb

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "webdav")
data class WebDavEntity(
    @PrimaryKey(autoGenerate = true) @ColumnInfo(name = "rowid") val id: Int = 0,
    @ColumnInfo(name = "name") val name: String,
    @ColumnInfo(name = "url") val url: String,
    @ColumnInfo(name = "login_id") val loginId: String,
    @ColumnInfo(name = "password") val password: String,
    @ColumnInfo(name = "is_active") val isActive: Boolean,
)