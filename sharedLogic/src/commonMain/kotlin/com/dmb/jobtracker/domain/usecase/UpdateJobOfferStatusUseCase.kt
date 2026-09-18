package com.dmb.jobtracker.domain.usecase

import com.rickclephas.kmp.nativecoroutines.NativeCoroutines
import com.dmb.jobtracker.domain.model.ApplicationStatus
import com.dmb.jobtracker.domain.model.JobOffer
import com.dmb.jobtracker.domain.repository.JobOfferRepository

class UpdateJobOfferStatusUseCase(private val repository: JobOfferRepository) {
    @NativeCoroutines
    suspend operator fun invoke(offer: JobOffer, newStatus: ApplicationStatus) {
        repository.update(offer.copy(status = newStatus))
    }
}
