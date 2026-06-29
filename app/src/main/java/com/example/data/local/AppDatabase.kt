package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [
        SearchHistoryEntity::class,
        LikedSongEntity::class,
        RecentlyPlayedEntity::class,
        DownloadedSongEntity::class,
        PlaylistEntity::class,
        PlaylistSongCrossRef::class,
        ChatMessageEntity::class,
        SongCacheEntity::class
    ],
    version = 3,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun searchHistoryDao(): SearchHistoryDao
    abstract fun likedSongDao(): LikedSongDao
    abstract fun recentlyPlayedDao(): RecentlyPlayedDao
    abstract fun downloadedSongDao(): DownloadedSongDao
    abstract fun playlistDao(): PlaylistDao
    abstract fun chatMessageDao(): ChatMessageDao
    abstract fun songCacheDao(): SongCacheDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "sepatify_database"
                )
                .fallbackToDestructiveMigration(true)
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
