package com.dmb.jobtracker.domain.usecase

import com.dmb.jobtracker.domain.model.JobOffer
import com.dmb.jobtracker.domain.repository.JobOfferRepository
import com.rickclephas.kmp.nativecoroutines.NativeCoroutines

internal class UpdateJobOfferUseCase(private val repository: JobOfferRepository) {

    suspend operator fun invoke(offer: JobOffer) {
        require(offer.title.isNotBlank()) { "Le titre du poste ne peut pas être vide" }
        require(offer.company.isNotBlank()) { "Le nom de l'entreprise ne peut pas être vide" }
        repository.update(offer)
    }
}