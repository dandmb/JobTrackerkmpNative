package com.dmb.jobtracker.data.repository


import com.dmb.jobtracker.data.local.dao.JobOfferDao
import com.dmb.jobtracker.data.mapper.toDomain
import com.dmb.jobtracker.data.mapper.toEntity
import com.dmb.jobtracker.domain.model.ApplicationStatus
import com.dmb.jobtracker.domain.model.JobOffer
import com.dmb.jobtracker.domain.repository.JobOfferRepository
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
}