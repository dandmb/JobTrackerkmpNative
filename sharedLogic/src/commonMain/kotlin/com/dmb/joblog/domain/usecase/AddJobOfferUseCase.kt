package com.dmb.joblog.domain.usecase

import com.rickclephas.kmp.nativecoroutines.NativeCoroutines
import com.dmb.joblog.domain.model.JobOffer
import com.dmb.joblog.domain.repository.JobOfferRepository


class AddJobOfferUseCase(private val repository: JobOfferRepository) {
    @NativeCoroutines
    suspend operator fun invoke(offer: JobOffer): Long {
        require(offer.title.isNotBlank()) { "The job title must not be blank" }
        require(offer.company.isNotBlank()) { "The company name must not be blank" }
        return repository.add(offer)
    }
}