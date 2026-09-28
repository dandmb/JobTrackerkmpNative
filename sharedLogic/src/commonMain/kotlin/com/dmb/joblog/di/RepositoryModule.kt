package com.dmb.joblog.di

import com.dmb.joblog.data.local.DeletedDataPurger
import com.dmb.joblog.data.repository.JobOfferRepositoryImpl
import com.dmb.joblog.domain.repository.JobOfferRepository
import org.koin.dsl.module

val repositoryModule = module {
    single<JobOfferRepository> {
        val purger = get<DeletedDataPurger>()
        JobOfferRepositoryImpl(dao = get(), purgeDeletedData = { purger.purge() })
    }
}
