package com.dmb.joblog.di

import com.dmb.joblog.data.repository.JobOfferRepositoryImpl
import com.dmb.joblog.domain.repository.JobOfferRepository
import org.koin.dsl.module

val repositoryModule = module {
    single<JobOfferRepository> { JobOfferRepositoryImpl(dao = get()) }
}
