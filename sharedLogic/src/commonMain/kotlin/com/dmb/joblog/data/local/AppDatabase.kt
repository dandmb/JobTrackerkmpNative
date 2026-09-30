package com.dmb.joblog.data.local

import androidx.room.ConstructedBy
import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.RoomDatabaseConstructor
import androidx.room.TypeConverters
import com.dmb.joblog.data.local.converter.Converters
import com.dmb.joblog.data.local.dao.AttachmentDao
import com.dmb.joblog.data.local.dao.JobOfferDao
import com.dmb.joblog.data.local.entity.AttachmentEntity
import com.dmb.joblog.data.local.entity.JobOfferAttachmentEntity
import com.dmb.joblog.data.local.entity.JobOfferEntity

@Database(entities = [JobOfferEntity::class, AttachmentEntity::class, JobOfferAttachmentEntity::class], version = 3, exportSchema = true)
@TypeConverters(Converters::class)
@ConstructedBy(AppDatabaseConstructor::class)
abstract class AppDatabase : RoomDatabase() {
    internal abstract fun jobOfferDao(): JobOfferDao
    internal abstract fun attachmentDao(): AttachmentDao
}

// Nécessaire pour Room KMP (génération par KSP sur chaque plateforme)
@Suppress("NO_ACTUAL_FOR_EXPECT")
expect object AppDatabaseConstructor : RoomDatabaseConstructor<AppDatabase> {
    override fun initialize(): AppDatabase
}