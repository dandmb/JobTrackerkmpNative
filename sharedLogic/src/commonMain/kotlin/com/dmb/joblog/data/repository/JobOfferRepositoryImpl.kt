package com.dmb.joblog.data.repository

import com.dmb.joblog.data.local.dao.JobOfferDao
import com.dmb.joblog.data.local.entity.JobOfferAttachmentEntity
import com.dmb.joblog.data.local.entity.JobOfferEntity
import com.dmb.joblog.data.mapper.attachmentIdsByRole
import com.dmb.joblog.data.mapper.toDomain
import com.dmb.joblog.data.mapper.toEntity
import com.dmb.joblog.domain.model.ApplicationStatus
import com.dmb.joblog.domain.model.JobOffer
import com.dmb.joblog.domain.repository.JobOfferRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

internal class JobOfferRepositoryImpl(
    private val dao: JobOfferDao,
    private val purgeDeletedData: suspend () -> Unit = {},
    private val onAttachmentsDetached: suspend (Set<Long>) -> Unit = {},
) : JobOfferRepository {

    override fun getAll(): Flow<List<JobOffer>> = withAttachments(dao.getAll())

    override fun getByStatus(status: ApplicationStatus): Flow<List<JobOffer>> = withAttachments(dao.getByStatus(status.name))

    private fun withAttachments(offers: Flow<List<JobOfferEntity>>): Flow<List<JobOffer>> =
        combine(offers, dao.getAllAttachmentLinks()) { entities, links ->
            val linksByOffer = links.groupBy { it.jobOfferId }
            entities.map { it.toDomain(linksByOffer[it.id].orEmpty()) }
        }

    override suspend fun add(offer: JobOffer): Long {
        val id = dao.insert(offer.toEntity())
        writeAttachmentLinks(id, offer)
        return id
    }

    override suspend fun update(offer: JobOffer) {
        val existing = dao.getById(offer.id)
        val previousAttachmentIds = dao.getAttachmentLinks(offer.id).map { it.attachmentId }.toSet()
        dao.update(offer.toEntity(existingCreatedAt = existing?.createdAtEpochMillis))
        writeAttachmentLinks(offer.id, offer)
        val detached = previousAttachmentIds - offer.attachmentIdsByRole().values.filterNotNull().toSet()
        if (detached.isNotEmpty()) onAttachmentsDetached(detached)
    }

    override suspend fun delete(offer: JobOffer) =
        dao.delete(offer.toEntity())

    override suspend fun getCreatedAt(id: Long): Long? =
        dao.getById(id)?.createdAtEpochMillis

    override suspend fun restore(offer: JobOffer, createdAtEpochMillis: Long) {
        val id = dao.insert(offer.toEntity(existingCreatedAt = createdAtEpochMillis))
        writeAttachmentLinks(id, offer)
    }

    private suspend fun writeAttachmentLinks(jobOfferId: Long, offer: JobOffer) {
        for ((role, attachmentId) in offer.attachmentIdsByRole()) {
            if (attachmentId != null && dao.attachmentExists(attachmentId) > 0) {
                dao.upsertAttachmentLink(JobOfferAttachmentEntity(jobOfferId = jobOfferId, role = role.name, attachmentId = attachmentId))
            } else {
                dao.deleteAttachmentLink(jobOfferId, role.name)
            }
        }
    }

    override suspend fun deleteAll() {
        dao.deleteAll()
        dao.deleteAllAttachments()
        purgeDeletedData()
    }
}
