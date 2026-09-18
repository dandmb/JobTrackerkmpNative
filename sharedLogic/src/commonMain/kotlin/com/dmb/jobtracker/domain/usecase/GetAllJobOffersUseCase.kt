package com.dmb.jobtracker.domain.usecase



import com.dmb.jobtracker.domain.model.JobOffer
import com.dmb.jobtracker.domain.repository.JobOfferRepository
import kotlinx.coroutines.flow.Flow

class GetAllJobOffersUseCase(private val repository: JobOfferRepository) {
    operator fun invoke(): Flow<List<JobOffer>> = repository.getAll()
}