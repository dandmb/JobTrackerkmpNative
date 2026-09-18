package com.dmb.jobtracker

import android.app.Application
import com.dmb.jobtracker.data.local.AndroidAppContextHolder
import com.dmb.jobtracker.di.sharedModules
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin

class MonApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        AndroidAppContextHolder.context = applicationContext
        startKoin {
            androidContext(this@MonApplication)
            modules(sharedModules())
        }
    }
}