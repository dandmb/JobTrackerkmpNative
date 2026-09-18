package com.dmb.jobtracker.di

import com.dmb.jobtracker.presentation.joboffer.JobOfferListViewModel
import org.koin.mp.KoinPlatform

class KoinHelper {
    fun jobOfferListViewModel(): JobOfferListViewModel =
        KoinPlatform.getKoin().get()

}