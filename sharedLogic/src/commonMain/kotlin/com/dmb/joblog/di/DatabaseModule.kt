package com.dmb.joblog.di

import com.dmb.joblog.data.local.AppDatabase
import com.dmb.joblog.data.local.DeletedDataPurger
import com.dmb.joblog.data.local.getDatabaseBuilder
import com.dmb.joblog.data.local.getRoomDatabase
import com.dmb.joblog.data.local.purgeDeletedData
import org.koin.dsl.module

val databaseModule = module {
    single { getRoomDatabase(getDatabaseBuilder()) }
    single { get<com.dmb.joblog.data.local.AppDatabase>().jobOfferDao() }
    single<DeletedDataPurger> {
        val database = get<AppDatabase>()
        DeletedDataPurger { database.purgeDeletedData() }
    }
}