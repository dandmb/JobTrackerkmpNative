package com.dmb.joblog.di

import org.koin.core.context.startKoin

fun doInitKoin() {
    startKoin {
        modules(sharedModules())
    }
}