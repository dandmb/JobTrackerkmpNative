package com.dmb.jobtracker.testutil

import com.dmb.jobtracker.domain.model.ApplicationStatus
import com.dmb.jobtracker.domain.model.JobOffer
import com.dmb.jobtracker.domain.repository.JobOfferRepository
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map

/**
 * Repository en mémoire : se comporte comme le vrai (l'id est attribué à l'ajout, `getAll` réémet à chaque
 * changement) et enregistre les appels pour que les tests puissent les vérifier.
 */
internal class FakeJobOfferRepository(initial: List<JobOffer> = emptyList()) : JobOfferRepository {

    val offers = MutableStateFlow(initial)

    val addCalls = mutableListOf<JobOffer>()
    val updateCalls = mutableListOf<JobOffer>()
    val deleteCalls = mutableListOf<JobOffer>()

    /** Si non nul, `add` / `update` / `delete` le lèvent (simule une erreur d'accès aux données). */
    var failure: Throwable? = null

    /** Si non nul, `add` / `update` / `delete` restent suspendus jusqu'à sa complétion (permet de tester l'annulation en cours d'action). */
    var gate: CompletableDeferred<Unit>? = null

    /** Remplace le flux renvoyé par `getAll` (ex. un flux qui échoue). */
    var getAllOverride: Flow<List<JobOffer>>? = null

    private var nextId = 1L

    override fun getAll(): Flow<List<JobOffer>> = getAllOverride ?: offers

    override fun getByStatus(status: ApplicationStatus): Flow<List<JobOffer>> =
        offers.map { list -> list.filter { it.status == status } }

    override suspend fun add(offer: JobOffer): Long {
        gate?.await()
        failure?.let { throw it }
        addCalls += offer
        val id = if (offer.id != 0L) offer.id else nextId++
        offers.value = offers.value + offer.copy(id = id)
        return id
    }

    override suspend fun update(offer: JobOffer) {
        gate?.await()
        failure?.let { throw it }
        updateCalls += offer
        offers.value = offers.value.map { if (it.id == offer.id) offer else it }
    }

    override suspend fun delete(offer: JobOffer) {
        gate?.await()
        failure?.let { throw it }
        deleteCalls += offer
        offers.value = offers.value.filterNot { it.id == offer.id }
    }
}
