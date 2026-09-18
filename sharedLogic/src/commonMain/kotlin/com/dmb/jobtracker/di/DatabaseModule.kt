package com.dmb.jobtracker.di


import com.dmb.jobtracker.data.local.getDatabaseBuilder
import com.dmb.jobtracker.data.local.getRoomDatabase
import org.koin.dsl.module

val databaseModule = module {
    single { getRoomDatabase(getDatabaseBuilder()) }
    single { get<com.dmb.jobtracker.data.local.AppDatabase>().jobOfferDao() }
}