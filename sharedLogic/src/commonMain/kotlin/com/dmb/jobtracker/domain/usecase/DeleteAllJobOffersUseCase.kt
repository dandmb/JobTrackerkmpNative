package com.dmb.jobtracker.domain.usecase

import com.rickclephas.kmp.nativecoroutines.NativeCoroutines
import com.dmb.jobtracker.domain.repository.JobOfferRepository

/** Efface toutes les candidatures de l'appareil (écran « À propos » → « Supprimer toutes mes données »). */
class DeleteAllJobOffersUseCase(private val repository: JobOfferRepository) {
    @NativeCoroutines
    suspend operator fun invoke(): Unit = repository.deleteAll()
}
