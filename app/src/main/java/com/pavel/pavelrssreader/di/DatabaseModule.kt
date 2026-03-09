package com.pavel.pavelrssreader.di

import android.content.Context
import androidx.room.Room
import com.pavel.pavelrssreader.data.db.AppDatabase
import com.pavel.pavelrssreader.data.db.dao.ArticleDao
import com.pavel.pavelrssreader.data.db.dao.FeedDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): AppDatabase =
        Room.databaseBuilder(context, AppDatabase::class.java, "rss_reader.db")
            .addMigrations(AppDatabase.MIGRATION_1_2)
            .build()

    @Provides
    @Singleton
    fun provideFeedDao(db: AppDatabase): FeedDao = db.feedDao()

    @Provides
    @Singleton
    fun provideArticleDao(db: AppDatabase): ArticleDao = db.articleDao()
}
