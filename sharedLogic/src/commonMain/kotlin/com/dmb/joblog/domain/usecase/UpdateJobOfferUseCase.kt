package com.dmb.joblog.domain.usecase

import com.dmb.joblog.domain.model.JobOffer
import com.dmb.joblog.domain.repository.JobOfferRepository
import com.rickclephas.kmp.nativecoroutines.NativeCoroutines

internal class UpdateJobOfferUseCase(private val repository: JobOfferRepository) {

    suspend operator fun invoke(offer: JobOffer) {
        require(offer.title.isNotBlank()) { "The job title must not be blank" }
        require(offer.company.isNotBlank()) { "The company name must not be blank" }
        repository.update(offer)
    }
}