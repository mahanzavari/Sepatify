package com.aistudio.sepatify

import android.app.Application
import com.aistudio.sepatify.di.appModule
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.context.startKoin
import org.koin.core.logger.Level

class SepatifyApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        startKoin {
            androidLogger(Level.ERROR)
            androidContext(this@SepatifyApplication)
            modules(appModule)
        }
    }
}
