package com.dmb.joblog.di

import com.dmb.joblog.data.local.DeletedDataPurger
import com.dmb.joblog.data.repository.AttachmentRepositoryImpl
import com.dmb.joblog.data.repository.JobOfferRepositoryImpl
import com.dmb.joblog.domain.repository.AttachmentRepository
import com.dmb.joblog.domain.repository.JobOfferRepository
import org.koin.dsl.module

val repositoryModule = module {
    single<AttachmentRepository> { AttachmentRepositoryImpl(dao = get(), fileStore = get()) }
    single<JobOfferRepository> {
        val purger = get<DeletedDataPurger>()
        val attachments = get<AttachmentRepository>()
        JobOfferRepositoryImpl(
            dao = get(),
            purgeDeletedData = { purger.purge() },
            onAttachmentsDetached = { attachments.deleteIfUnusedOneTime(it) },
            inTransaction = get(),
        )
    }
}
