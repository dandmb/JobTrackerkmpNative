package com.dmb.jobtracker.di


import com.dmb.jobtracker.domain.usecase.AddJobOfferUseCase
import com.dmb.jobtracker.domain.usecase.DeleteJobOfferUseCase
import com.dmb.jobtracker.domain.usecase.GetAllJobOffersUseCase
import com.dmb.jobtracker.domain.usecase.UpdateJobOfferStatusUseCase
import org.koin.dsl.module

val useCaseModule = module {
    factory { GetAllJobOffersUseCase(get()) }
    factory { AddJobOfferUseCase(get()) }
    factory { UpdateJobOfferStatusUseCase(get()) }
    factory { DeleteJobOfferUseCase(get()) }
}