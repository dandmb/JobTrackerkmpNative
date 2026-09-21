package com.dmb.joblog

import android.app.Application
import com.dmb.joblog.data.local.AndroidAppContextHolder
import com.dmb.joblog.di.sharedModules
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