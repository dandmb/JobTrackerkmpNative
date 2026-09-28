package com.dmb.joblog.data.local

import androidx.room.ConstructedBy
import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.RoomDatabaseConstructor
import androidx.room.TypeConverters
import com.dmb.joblog.data.local.converter.Converters
import com.dmb.joblog.data.local.dao.JobOfferDao
import com.dmb.joblog.data.local.entity.JobOfferEntity


@Database(entities = [JobOfferEntity::class], version = 2, exportSchema = true)
@TypeConverters(Converters::class)
@ConstructedBy(AppDatabaseConstructor::class)
abstract class AppDatabase : RoomDatabase() {
    internal abstract fun jobOfferDao(): JobOfferDao
}

// Nécessaire pour Room KMP (génération par KSP sur chaque plateforme)
@Suppress("NO_ACTUAL_FOR_EXPECT")
expect object AppDatabaseConstructor : RoomDatabaseConstructor<AppDatabase> {
    override fun initialize(): AppDatabase
}