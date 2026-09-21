package com.dmb.joblog.data.repository


import com.dmb.joblog.data.local.dao.JobOfferDao
import com.dmb.joblog.data.mapper.toDomain
import com.dmb.joblog.data.mapper.toEntity
import com.dmb.joblog.domain.model.ApplicationStatus
import com.dmb.joblog.domain.model.JobOffer
import com.dmb.joblog.domain.repository.JobOfferRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

internal class JobOfferRepositoryImpl(
    private val dao: JobOfferDao
) : JobOfferRepository {

    override fun getAll(): Flow<List<JobOffer>> =
        dao.getAll().map { list -> list.map { it.toDomain() } }

    override fun getByStatus(status: ApplicationStatus): Flow<List<JobOffer>> =
        dao.getByStatus(status.name).map { list -> list.map { it.toDomain() } }

    override suspend fun add(offer: JobOffer): Long =
        dao.insert(offer.toEntity())

    override suspend fun update(offer: JobOffer) {
        val existing = dao.getById(offer.id)
        dao.update(offer.toEntity(existingCreatedAt = existing?.createdAtEpochMillis))
    }

    override suspend fun delete(offer: JobOffer) =
        dao.delete(offer.toEntity())

    override suspend fun getCreatedAt(id: Long): Long? =
        dao.getById(id)?.createdAtEpochMillis

    override suspend fun restore(offer: JobOffer, createdAtEpochMillis: Long) {
        dao.insert(offer.toEntity(existingCreatedAt = createdAtEpochMillis))
    }

    override suspend fun deleteAll() =
        dao.deleteAll()
}