package com.dmb.joblog.domain.usecase

import com.rickclephas.kmp.nativecoroutines.NativeCoroutines
import com.dmb.joblog.domain.model.DeletedJobOffer
import com.dmb.joblog.domain.model.JobOffer
import com.dmb.joblog.domain.repository.JobOfferRepository

/** Suppression d'une candidature ET son inverse (« Annuler ») : les deux vont ensemble pour que la restauration soit fidèle. */
class DeleteJobOfferUseCase(private val repository: JobOfferRepository) {

    /** Supprime l'offre et renvoie de quoi la restaurer à l'identique (offre + horodatage de création d'origine). */
    @NativeCoroutines
    suspend operator fun invoke(offer: JobOffer): DeletedJobOffer {
        val createdAt = repository.getCreatedAt(offer.id)   // lu AVANT la suppression : après, la ligne n'existe plus
        repository.delete(offer)
        return DeletedJobOffer(offer, createdAt)
    }

    /**
     * Ajoute l'offre SEULEMENT si elle est absente de la base (la base, pas l'affichage, fait foi) : repli d'un « Annuler »
     * dont la suppression n'est pas connue. Ne touche jamais une offre présente (la ré-insérer la réhorodaterait).
     */
    @NativeCoroutines
    suspend fun addIfMissing(offer: JobOffer) {
        if (repository.getCreatedAt(offer.id) == null) repository.add(offer)
    }

    /**
     * Annule une suppression : ré-insère l'offre avec son horodatage d'origine (même position dans la liste triée).
     * Sans horodatage connu (l'offre n'existait pas), retombe sur un ajout simple. Aucune validation : on restaure
     * exactement ce qui existait, y compris une donnée héritée jugée invalide par les règles actuelles.
     */
    @NativeCoroutines
    suspend fun restore(deleted: DeletedJobOffer) {
        val createdAt = deleted.createdAtEpochMillis
        if (createdAt != null) repository.restore(deleted.offer, createdAt) else repository.add(deleted.offer)
    }
}
