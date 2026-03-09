package com.pavel.pavelrssreader.di

import com.pavel.pavelrssreader.data.repository.RssRepositoryImpl
import com.pavel.pavelrssreader.data.repository.SettingsRepositoryImpl
import com.pavel.pavelrssreader.domain.repository.RssRepository
import com.pavel.pavelrssreader.domain.repository.SettingsRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindRssRepository(impl: RssRepositoryImpl): RssRepository

    @Binds
    @Singleton
    abstract fun bindSettingsRepository(impl: SettingsRepositoryImpl): SettingsRepository
}
