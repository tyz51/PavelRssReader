package com.pavel.pavelrssreader.domain.model

private const val TTL_MS = 24 * 60 * 60 * 1000L

data class Article(
    val id: Long = 0L,
    val feedId: Long,
    val guid: String,
    val title: String,
    val link: String,
    val description: String,
    val publishedAt: Long,
    val fetchedAt: Long = System.currentTimeMillis(),
    val isRead: Boolean = false,
    val isFavorite: Boolean = false,
    val imageUrl: String? = null,
    val sourceName: String = ""
) {
    fun isExpired(now: Long = System.currentTimeMillis()): Boolean =
        !isFavorite && (now - fetchedAt) > TTL_MS
}
