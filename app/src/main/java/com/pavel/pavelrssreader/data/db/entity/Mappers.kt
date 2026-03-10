package com.pavel.pavelrssreader.data.db.entity

import com.pavel.pavelrssreader.domain.model.Article
import com.pavel.pavelrssreader.domain.model.Feed

fun FeedEntity.toDomain() = Feed(id = id, url = url, title = title, addedAt = addedAt)
fun Feed.toEntity() = FeedEntity(id = id, url = url, title = title, addedAt = addedAt)

fun ArticleEntity.toDomain() = Article(
    id = id, feedId = feedId, guid = guid, title = title, link = link,
    description = description, publishedAt = publishedAt, fetchedAt = fetchedAt,
    isRead = isRead, isFavorite = isFavorite, imageUrl = imageUrl, sourceName = sourceName
)

fun Article.toEntity() = ArticleEntity(
    id = id, feedId = feedId, guid = guid, title = title, link = link,
    description = description, publishedAt = publishedAt, fetchedAt = fetchedAt,
    isRead = isRead, isFavorite = isFavorite, imageUrl = imageUrl, sourceName = sourceName
)
