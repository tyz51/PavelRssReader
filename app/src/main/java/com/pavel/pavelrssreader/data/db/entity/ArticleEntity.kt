package com.pavel.pavelrssreader.data.db.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "articles",
    foreignKeys = [ForeignKey(
        entity = FeedEntity::class,
        parentColumns = ["id"],
        childColumns = ["feedId"],
        onDelete = ForeignKey.CASCADE
    )],
    indices = [Index("feedId"), Index(value = ["feedId", "guid"], unique = true)]
)
data class ArticleEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val feedId: Long,
    val guid: String,
    val title: String,
    val link: String,
    val description: String,
    val publishedAt: Long,
    val fetchedAt: Long,
    val isRead: Boolean = false,
    val isFavorite: Boolean = false,
    val imageUrl: String? = null,
    val sourceName: String = ""
)
