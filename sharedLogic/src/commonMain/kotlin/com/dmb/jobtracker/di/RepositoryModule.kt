package com.dmb.jobtracker.di

import com.dmb.jobtracker.data.repository.JobOfferRepositoryImpl
import com.dmb.jobtracker.domain.repository.JobOfferRepository
import org.koin.dsl.module

val repositoryModule = module {
    single<JobOfferRepository> { JobOfferRepositoryImpl(dao = get()) }
}
