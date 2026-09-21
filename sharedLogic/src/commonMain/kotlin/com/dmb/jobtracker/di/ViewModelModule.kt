package com.dmb.jobtracker.di


import com.dmb.jobtracker.presentation.about.AboutViewModel
import com.dmb.jobtracker.presentation.joboffer.JobOfferListViewModel
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
