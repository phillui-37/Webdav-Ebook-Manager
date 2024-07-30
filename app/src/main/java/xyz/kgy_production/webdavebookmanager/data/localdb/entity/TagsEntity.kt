package xyz.kgy_production.webdavebookmanager.data.localdb.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "tags")
data class TagsEntity(
    @PrimaryKey val tag: String
)
