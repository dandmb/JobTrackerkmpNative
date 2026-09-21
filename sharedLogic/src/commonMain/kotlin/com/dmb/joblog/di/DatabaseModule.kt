package com.dmb.joblog.di


import com.dmb.joblog.data.local.getDatabaseBuilder
import com.dmb.joblog.data.local.getRoomDatabase
import org.koin.dsl.module

val databaseModule = module {
    single { getRoomDatabase(getDatabaseBuilder()) }
    single { get<com.dmb.joblog.data.local.AppDatabase>().jobOfferDao() }
}