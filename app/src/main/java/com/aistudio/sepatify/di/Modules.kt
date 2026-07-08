package com.aistudio.sepatify.di

import androidx.work.WorkManager
import com.aistudio.sepatify.data.local.AppDatabase
import com.aistudio.sepatify.data.local.PreferencesManager
import com.aistudio.sepatify.data.repository.*
import com.aistudio.sepatify.ui.viewmodel.*
import org.koin.android.ext.koin.androidContext
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module

val appModule = module {
    // Database and Preferences
    single { AppDatabase.getDatabase(androidContext()) }
    single { get<AppDatabase>().searchHistoryDao() }
    single { get<AppDatabase>().likedSongDao() }
    single { get<AppDatabase>().recentlyPlayedDao() }
    single { get<AppDatabase>().downloadedSongDao() }
    single { get<AppDatabase>().playlistDao() }
    single { get<AppDatabase>().chatMessageDao() }
    single { get<AppDatabase>().songCacheDao() }

    single { PreferencesManager(androidContext()) }
    single { com.aistudio.sepatify.player.AudioPlayerManager(androidContext()) }
    single { WorkManager.getInstance(androidContext()) }

    // Repositories
    single<AuthRepository> { AuthRepositoryImpl() }
    single<SongRepository> { SongRepositoryImpl(androidContext(), get(), get(), get(), get(), get()) }
    single<ChatRepository> { ChatRepositoryImpl(get(), get()) }
    single<DownloadRepository> { DownloadRepositoryImpl(androidContext(), get(), get()) }

    // ViewModels
    viewModel { MainViewModel(get(), get()) }
    viewModel { AuthViewModel(get(), get()) }
    viewModel { HomeViewModel(get()) }
    viewModel { SearchViewModel(get(), get()) }
    viewModel { DownloadViewModel(get()) }
    viewModel { PlaylistViewModel(get()) }
    viewModel { ChatViewModel(get(), get()) }
    viewModel { SharedAudioViewModel(get(), get(), get()) }
}
