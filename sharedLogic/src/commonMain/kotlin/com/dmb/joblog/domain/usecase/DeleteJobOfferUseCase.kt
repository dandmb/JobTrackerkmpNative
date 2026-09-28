package com.dmb.joblog.domain.usecase

import com.rickclephas.kmp.nativecoroutines.NativeCoroutines
import com.dmb.joblog.domain.model.DeletedJobOffer
import com.dmb.joblog.domain.model.JobOffer
import com.dmb.joblog.domain.repository.JobOfferRepository

class DeleteJobOfferUseCase(private val repository: JobOfferRepository) {

    @NativeCoroutines
    suspend operator fun invoke(offer: JobOffer): DeletedJobOffer {
        val createdAt = repository.getCreatedAt(offer.id)
        repository.delete(offer)
        return DeletedJobOffer(offer, createdAt)
    }

    @NativeCoroutines
    suspend fun addIfMissing(offer: JobOffer) {
        if (repository.getCreatedAt(offer.id) == null) repository.add(offer)
    }

    @NativeCoroutines
    suspend fun restore(deleted: DeletedJobOffer) {
        val createdAt = deleted.createdAtEpochMillis
        if (createdAt != null) repository.restore(deleted.offer, createdAt) else repository.add(deleted.offer)
    }
}
