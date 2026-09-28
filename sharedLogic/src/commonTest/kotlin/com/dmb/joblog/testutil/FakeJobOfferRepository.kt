package com.dmb.joblog.testutil

import com.dmb.joblog.domain.model.ApplicationStatus
import com.dmb.joblog.domain.model.JobOffer
import com.dmb.joblog.domain.repository.JobOfferRepository
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map

internal class FakeJobOfferRepository(initial: List<JobOffer> = emptyList()) : JobOfferRepository {

    val offers = MutableStateFlow(initial)

    val addCalls = mutableListOf<JobOffer>()
    val updateCalls = mutableListOf<JobOffer>()
    val deleteCalls = mutableListOf<JobOffer>()
    var deleteAllCalls = 0
        private set

    val createdAtById = mutableMapOf<Long, Long>()
    val restoreCalls = mutableListOf<Pair<JobOffer, Long>>()
    private var nextCreatedAt = 1_000L

    init {
        initial.forEach { createdAtById[it.id] = nextCreatedAt++ }
    }

    var failure: Throwable? = null

    var gate: CompletableDeferred<Unit>? = null

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
        createdAtById[id] = nextCreatedAt++
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
        createdAtById.remove(offer.id)
        offers.value = offers.value.filterNot { it.id == offer.id }
    }

    override suspend fun getCreatedAt(id: Long): Long? = createdAtById[id]

    override suspend fun restore(offer: JobOffer, createdAtEpochMillis: Long) {
        gate?.await()
        failure?.let { throw it }
        restoreCalls += offer to createdAtEpochMillis
        createdAtById[offer.id] = createdAtEpochMillis
        offers.value = offers.value.filterNot { it.id == offer.id } + offer
    }

    override suspend fun deleteAll() {
        gate?.await()
        failure?.let { throw it }
        deleteAllCalls++
        createdAtById.clear()
        offers.value = emptyList()
    }
}
