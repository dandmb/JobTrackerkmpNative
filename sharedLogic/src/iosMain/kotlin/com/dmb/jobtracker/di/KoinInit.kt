package com.dmb.jobtracker.di

import org.koin.core.context.startKoin

fun doInitKoin() {
    startKoin {
        modules(sharedModules())
    }
}