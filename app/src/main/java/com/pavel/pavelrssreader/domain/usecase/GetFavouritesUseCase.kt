package com.pavel.pavelrssreader.domain.usecase

import com.pavel.pavelrssreader.domain.repository.RssRepository
import javax.inject.Inject

class GetFavouritesUseCase @Inject constructor(private val repo: RssRepository) {
    operator fun invoke() = repo.getFavouriteArticles()
}
