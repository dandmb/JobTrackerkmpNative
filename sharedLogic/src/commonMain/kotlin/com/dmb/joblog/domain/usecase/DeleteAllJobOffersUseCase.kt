package com.dmb.joblog.domain.usecase

import com.rickclephas.kmp.nativecoroutines.NativeCoroutines
import com.dmb.joblog.domain.repository.JobOfferRepository

class DeleteAllJobOffersUseCase(private val repository: JobOfferRepository) {
    @NativeCoroutines
    suspend operator fun invoke(): Unit = repository.deleteAll()
}
