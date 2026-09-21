package com.dmb.joblog.domain.usecase

import com.rickclephas.kmp.nativecoroutines.NativeCoroutines
import com.dmb.joblog.domain.repository.JobOfferRepository

/** Efface toutes les candidatures de l'appareil (écran « À propos » → « Supprimer toutes mes données »). */
class DeleteAllJobOffersUseCase(private val repository: JobOfferRepository) {
    @NativeCoroutines
    suspend operator fun invoke(): Unit = repository.deleteAll()
}
