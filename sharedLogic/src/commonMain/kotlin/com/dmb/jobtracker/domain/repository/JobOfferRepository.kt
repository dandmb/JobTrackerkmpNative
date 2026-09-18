package com.dmb.jobtracker.domain.repository

import com.dmb.jobtracker.domain.model.ApplicationStatus
import kotlinx.coroutines.flow.Flow

import com.dmb.jobtracker.domain.model.JobOffer

interface JobOfferRepository {
    fun getAll(): Flow<List<JobOffer>>
    fun getByStatus(status: ApplicationStatus): Flow<List<JobOffer>>
    suspend fun add(offer: JobOffer): Long
    suspend fun update(offer: JobOffer)
    suspend fun delete(offer: JobOffer)
}