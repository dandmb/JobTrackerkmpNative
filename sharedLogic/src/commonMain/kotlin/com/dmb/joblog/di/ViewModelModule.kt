package com.dmb.joblog.di

import com.dmb.joblog.domain.repository.AttachmentRepository
import com.dmb.joblog.presentation.about.AboutViewModel
import com.dmb.joblog.presentation.attachments.AttachmentsViewModel
import com.dmb.joblog.presentation.joboffer.JobOfferListViewModel
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.koin.dsl.module

internal class StartupAttachmentCleanUp(private val cleanUp: suspend () -> Unit) {
    private val mutex = Mutex()
    private var done = false

    suspend operator fun invoke() = mutex.withLock {
        if (!done) {
            done = true
            cleanUp()
        }
    }
}

val viewModelModule = module {
    single { StartupAttachmentCleanUp(cleanUp = get<AttachmentRepository>()::cleanUp) }
    factory {
        val startupCleanUp = get<StartupAttachmentCleanUp>()
        JobOfferListViewModel(
            getAllJobOffers = get(),
            addJobOffer = get(),
            updateJobOffer = get(),
            deleteJobOffer = get(),
            cleanUpAttachments = { startupCleanUp() },
        )
    }
    factory { AboutViewModel(deleteAllJobOffers = get()) }
    factory { AttachmentsViewModel(repository = get()) }
}
