package com.dmb.jobtracker.domain.repository

import com.rickclephas.kmp.nativecoroutines.NativeCoroutines
import com.dmb.jobtracker.domain.model.ApplicationStatus
import kotlinx.coroutines.flow.Flow
import com.dmb.jobtracker.domain.model.JobOffer

interface JobOfferRepository {
    @NativeCoroutines
    fun getAll(): Flow<List<JobOffer>>
    @NativeCoroutines
    fun getByStatus(status: ApplicationStatus): Flow<List<JobOffer>>
    @NativeCoroutines
    suspend fun add(offer: JobOffer): Long
    @NativeCoroutines
    suspend fun update(offer: JobOffer)
    @NativeCoroutines
    suspend fun delete(offer: JobOffer)
}
