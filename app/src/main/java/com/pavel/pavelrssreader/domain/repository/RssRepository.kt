package com.pavel.pavelrssreader.domain.repository

import com.pavel.pavelrssreader.data.db.dao.FeedUnreadCount
import com.pavel.pavelrssreader.domain.model.Article
import com.pavel.pavelrssreader.domain.model.Feed
import com.pavel.pavelrssreader.domain.model.Result
import kotlinx.coroutines.flow.Flow

interface RssRepository {
    fun getAllArticles(): Flow<List<Article>>
    fun getFavouriteArticles(): Flow<List<Article>>
    fun getAllFeeds(): Flow<List<Feed>>
    suspend fun addFeed(url: String): Result<Feed>
    suspend fun deleteFeed(feedId: Long)
    suspend fun refreshFeed(feed: Feed): Result<Unit>
    suspend fun refreshAllFeeds(): Result<Unit>
    suspend fun setFavourite(articleId: Long, isFavorite: Boolean)
    suspend fun markAsRead(articleId: Long)
    fun getUnreadCountsPerFeed(): Flow<List<FeedUnreadCount>>
}
