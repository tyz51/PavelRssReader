package com.pavel.pavelrssreader.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.pavel.pavelrssreader.data.db.entity.FeedEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface FeedDao {
    @Query("SELECT * FROM feeds ORDER BY addedAt DESC")
    fun getAllFeeds(): Flow<List<FeedEntity>>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertFeed(feed: FeedEntity): Long

    @Query("DELETE FROM feeds WHERE id = :feedId")
    suspend fun deleteFeed(feedId: Long)

    @Query("SELECT * FROM feeds")
    suspend fun getAllFeedsOneShot(): List<FeedEntity>
}
