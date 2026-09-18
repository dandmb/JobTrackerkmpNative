package com.dmb.jobtracker.domain.usecase

import com.dmb.jobtracker.domain.model.JobOffer
import com.dmb.jobtracker.domain.repository.JobOfferRepository


class DeleteJobOfferUseCase(private val repository: JobOfferRepository) {
    suspend operator fun invoke(offer: JobOffer) = repository.delete(offer)
}