package com.dmb.joblog.testutil

import com.dmb.joblog.data.local.dao.JobOfferDao
import com.dmb.joblog.data.local.entity.JobOfferAttachmentEntity
import com.dmb.joblog.data.local.entity.JobOfferEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map

internal class FakeJobOfferDao : JobOfferDao {

    val entities = MutableStateFlow<List<JobOfferEntity>>(emptyList())
    val links = MutableStateFlow<List<JobOfferAttachmentEntity>>(emptyList())
    val existingAttachmentIds = mutableSetOf<Long>()
    var deleteAllAttachmentsCalls = 0
        private set

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
        links.value = links.value.filterNot { it.jobOfferId == offer.id }
    }

    override suspend fun deleteAll() {
        deleteAllCalls++
        entities.value = emptyList()
        links.value = emptyList()
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

    override fun getAllAttachmentLinks(): Flow<List<JobOfferAttachmentEntity>> = links

    override suspend fun getAttachmentLinks(jobOfferId: Long): List<JobOfferAttachmentEntity> =
        links.value.filter { it.jobOfferId == jobOfferId }

    override suspend fun upsertAttachmentLink(link: JobOfferAttachmentEntity) {
        links.value = links.value.filterNot { it.jobOfferId == link.jobOfferId && it.role == link.role } + link
    }

    override suspend fun deleteAttachmentLink(jobOfferId: Long, role: String) {
        links.value = links.value.filterNot { it.jobOfferId == jobOfferId && it.role == role }
    }

    override suspend fun attachmentExists(attachmentId: Long): Int = if (attachmentId in existingAttachmentIds) 1 else 0

    override suspend fun deleteAllAttachments() {
        deleteAllAttachmentsCalls++
        existingAttachmentIds.clear()
    }
}
