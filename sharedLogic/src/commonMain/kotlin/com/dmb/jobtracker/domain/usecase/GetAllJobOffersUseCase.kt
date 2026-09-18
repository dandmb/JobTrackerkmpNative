package com.dmb.jobtracker.domain.usecase

import com.rickclephas.kmp.nativecoroutines.NativeCoroutines
import com.dmb.jobtracker.domain.model.JobOffer
import com.dmb.jobtracker.domain.repository.JobOfferRepository
import kotlinx.coroutines.flow.Flow

class GetAllJobOffersUseCase(private val repository: JobOfferRepository) {
    @NativeCoroutines
    operator fun invoke(): Flow<List<JobOffer>> = repository.getAll()
}
