package com.pavel.pavelrssreader.domain.usecase

import com.pavel.pavelrssreader.data.db.dao.FeedUnreadCount
import com.pavel.pavelrssreader.domain.repository.RssRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetUnreadCountsPerFeedUseCase @Inject constructor(private val repo: RssRepository) {
    operator fun invoke(): Flow<List<FeedUnreadCount>> = repo.getUnreadCountsPerFeed()
}
