package com.dmb.jobtracker.domain.usecase

import com.dmb.jobtracker.domain.model.JobOffer
import com.dmb.jobtracker.domain.repository.JobOfferRepository


class AddJobOfferUseCase(private val repository: JobOfferRepository) {
    suspend operator fun invoke(offer: JobOffer): Long {
        require(offer.title.isNotBlank()) { "Le titre du poste ne peut pas être vide" }
        require(offer.company.isNotBlank()) { "Le nom de l'entreprise ne peut pas être vide" }
        return repository.add(offer)
    }
}