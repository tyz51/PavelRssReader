package com.pavel.pavelrssreader.domain.usecase

import com.pavel.pavelrssreader.domain.repository.RssRepository
import javax.inject.Inject

class AddFeedUseCase @Inject constructor(private val repo: RssRepository) {
    suspend operator fun invoke(url: String) = repo.addFeed(url)
}
