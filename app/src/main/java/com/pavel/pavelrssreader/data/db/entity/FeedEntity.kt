package com.pavel.pavelrssreader.data.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "feeds")
data class FeedEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val url: String,
    val title: String,
    val addedAt: Long
)
