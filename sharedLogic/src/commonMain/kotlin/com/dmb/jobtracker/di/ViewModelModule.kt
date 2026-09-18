package com.dmb.jobtracker.di


import com.dmb.jobtracker.presentation.joboffer.JobOfferListViewModel
import org.koin.dsl.module

val viewModelModule = module {
    factory {
        JobOfferListViewModel(
            getAllJobOffers = get(),
            addJobOffer = get(),
            updateStatus = get(),
            deleteJobOffer = get()
        )
    }
}