package com.pavel.pavelrssreader.di

import com.pavel.pavelrssreader.data.network.RssApiService
import com.pavel.pavelrssreader.data.network.RssParser
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.scalars.ScalarsConverterFactory
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    @Provides
    @Singleton
    fun provideOkHttpClient(): OkHttpClient =
        OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .build()

    @Provides
    @Singleton
    fun provideRetrofit(client: OkHttpClient): Retrofit =
        Retrofit.Builder()
            .baseUrl("https://placeholder.invalid/")
            .addConverterFactory(ScalarsConverterFactory.create())
            .client(client)
            .build()

    @Provides
    @Singleton
    fun provideRssApiService(retrofit: Retrofit): RssApiService =
        retrofit.create(RssApiService::class.java)

    @Provides
    @Singleton
    fun provideRssParser(): RssParser = RssParser()
}
