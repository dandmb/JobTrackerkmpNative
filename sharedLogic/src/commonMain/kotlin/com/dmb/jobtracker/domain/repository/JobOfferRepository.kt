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
    /** Horodatage de création STOCKÉ de l'offre (null si elle n'existe pas) : sert à restaurer fidèlement une offre supprimée. */
    @NativeCoroutines
    suspend fun getCreatedAt(id: Long): Long?
    /**
     * Ré-insère une offre supprimée en CONSERVANT son horodatage de création d'origine : elle retrouve exactement sa
     * position dans la liste triée par date de création (contrairement à [add], qui horodate à l'instant présent).
     */
    @NativeCoroutines
    suspend fun restore(offer: JobOffer, createdAtEpochMillis: Long)
    /** Efface TOUTES les candidatures (droit à l'effacement : « Supprimer toutes mes données »). */
    @NativeCoroutines
    suspend fun deleteAll()
}
