package com.dmb.joblog.di


import com.dmb.joblog.domain.usecase.AddJobOfferUseCase
import com.dmb.joblog.domain.usecase.DeleteAllJobOffersUseCase
import com.dmb.joblog.domain.usecase.DeleteJobOfferUseCase
import com.dmb.joblog.domain.usecase.GetAllJobOffersUseCase
import com.dmb.joblog.domain.usecase.UpdateJobOfferUseCase
import org.koin.dsl.module

val useCaseModule = module {
    factory { GetAllJobOffersUseCase(get()) }
    factory { AddJobOfferUseCase(get()) }
    factory { UpdateJobOfferUseCase(get()) }
    factory { DeleteJobOfferUseCase(get()) }
    factory { DeleteAllJobOffersUseCase(get()) }
}