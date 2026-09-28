package com.dmb.joblog.testutil

import com.dmb.joblog.data.local.dao.JobOfferDao
import com.dmb.joblog.data.local.entity.JobOfferEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map

/**
 * DAO en mémoire reproduisant les contrats de Room : `insert` avec id auto-généré quand id == 0 et
 * `OnConflictStrategy.REPLACE`, `update` / `delete` par clé primaire, `getAll` trié par `createdAtEpochMillis`
 * décroissant.
 */
internal class FakeJobOfferDao : JobOfferDao {

    val entities = MutableStateFlow<List<JobOfferEntity>>(emptyList())

    val inserted = mutableListOf<JobOfferEntity>()
    val updated = mutableListOf<JobOfferEntity>()
    val deleted = mutableListOf<JobOfferEntity>()
    var deleteAllCalls = 0
        private set
    val getByIdCalls = mutableListOf<Long>()
    val getByStatusCalls = mutableListOf<String>()

    private var nextId = 1L

    override suspend fun insert(offer: JobOfferEntity): Long {
        inserted += offer
        val id = if (offer.id != 0L) offer.id else nextId++
        entities.value = entities.value.filterNot { it.id == id } + offer.copy(id = id)
        return id
    }

    override suspend fun update(offer: JobOfferEntity) {
        updated += offer
        entities.value = entities.value.map { if (it.id == offer.id) offer else it }
    }

    override suspend fun delete(offer: JobOfferEntity) {
        deleted += offer
        entities.value = entities.value.filterNot { it.id == offer.id }
    }

    override suspend fun deleteAll() {
        deleteAllCalls++
        entities.value = emptyList()
    }

    override fun getAll(): Flow<List<JobOfferEntity>> =
        entities.map { list -> list.sortedByDescending { it.createdAtEpochMillis } }

    override fun getByStatus(status: String): Flow<List<JobOfferEntity>> {
        getByStatusCalls += status
        return entities.map { list ->
            list.filter { it.status.name == status }.sortedByDescending { it.createdAtEpochMillis }
        }
    }

    override suspend fun getById(id: Long): JobOfferEntity? {
        getByIdCalls += id
        return entities.value.firstOrNull { it.id == id }
    }
}
