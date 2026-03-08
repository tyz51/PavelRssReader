package com.pavel.pavelrssreader.domain.usecase

import com.pavel.pavelrssreader.domain.repository.RssRepository
import javax.inject.Inject

class DeleteFeedUseCase @Inject constructor(private val repo: RssRepository) {
    suspend operator fun invoke(feedId: Long) = repo.deleteFeed(feedId)
}
