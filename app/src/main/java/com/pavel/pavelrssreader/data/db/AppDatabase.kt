package com.pavel.pavelrssreader.data.db

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.pavel.pavelrssreader.data.db.dao.ArticleDao
import com.pavel.pavelrssreader.data.db.dao.FeedDao
import com.pavel.pavelrssreader.data.db.entity.ArticleEntity
import com.pavel.pavelrssreader.data.db.entity.FeedEntity

@Database(
    entities = [FeedEntity::class, ArticleEntity::class],
    version = 2,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun feedDao(): FeedDao
    abstract fun articleDao(): ArticleDao

    companion object {
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE articles ADD COLUMN imageUrl TEXT")
            }
        }
    }
}
