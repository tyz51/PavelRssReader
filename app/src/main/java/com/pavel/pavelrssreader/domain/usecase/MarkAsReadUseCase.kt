package com.pavel.pavelrssreader.domain.usecase

import com.pavel.pavelrssreader.domain.repository.RssRepository
import javax.inject.Inject

class MarkAsReadUseCase @Inject constructor(private val repo: RssRepository) {
    suspend operator fun invoke(articleId: Long) = repo.markAsRead(articleId)
}
