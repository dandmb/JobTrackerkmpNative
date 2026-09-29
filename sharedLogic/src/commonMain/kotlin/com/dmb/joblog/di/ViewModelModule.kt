package com.dmb.joblog.di

import com.dmb.joblog.domain.repository.AttachmentRepository
import com.dmb.joblog.presentation.about.AboutViewModel
import com.dmb.joblog.presentation.attachments.AttachmentsViewModel
import com.dmb.joblog.presentation.joboffer.JobOfferListViewModel
import org.koin.dsl.module

val viewModelModule = module {
    factory {
        val attachments = get<AttachmentRepository>()
        JobOfferListViewModel(
            getAllJobOffers = get(),
            addJobOffer = get(),
            updateJobOffer = get(),
            deleteJobOffer = get(),
            cleanUpAttachments = { attachments.cleanUp() },
        )
    }
    factory { AboutViewModel(deleteAllJobOffers = get()) }
    factory { AttachmentsViewModel(repository = get()) }
}
