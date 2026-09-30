package com.dmb.joblog.di

import com.dmb.joblog.data.files.AttachmentFileStore
import com.dmb.joblog.data.files.createAttachmentFileStore
import com.dmb.joblog.data.local.AppDatabase
import com.dmb.joblog.data.local.DatabaseTransaction
import com.dmb.joblog.data.local.DeletedDataPurger
import com.dmb.joblog.data.local.getDatabaseBuilder
import com.dmb.joblog.data.local.getRoomDatabase
import com.dmb.joblog.data.local.purgeDeletedData
import com.dmb.joblog.data.local.writeTransaction
import org.koin.dsl.module

val databaseModule = module {
    single { getRoomDatabase(getDatabaseBuilder()) }
    single { get<AppDatabase>().jobOfferDao() }
    single { get<AppDatabase>().attachmentDao() }
    single<AttachmentFileStore> { createAttachmentFileStore() }
    single<DatabaseTransaction> { get<AppDatabase>().writeTransaction() }
    single<DeletedDataPurger> {
        val database = get<AppDatabase>()
        val fileStore = get<AttachmentFileStore>()
        DeletedDataPurger {
            fileStore.deleteAll()
            database.purgeDeletedData()
        }
    }
}
