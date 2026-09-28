package com.dmb.joblog.di


import com.dmb.joblog.presentation.about.AboutViewModel
import com.dmb.joblog.presentation.joboffer.JobOfferListViewModel
import org.koin.dsl.module

val viewModelModule = module {
    factory {
        JobOfferListViewModel(
            getAllJobOffers = get(),
            addJobOffer = get(),
            updateJobOffer = get(),
            deleteJobOffer = get()
        )
    }
    factory { AboutViewModel(deleteAllJobOffers = get()) }
}
