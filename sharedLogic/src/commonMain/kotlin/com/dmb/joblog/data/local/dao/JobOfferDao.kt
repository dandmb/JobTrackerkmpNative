package com.dmb.joblog.data.local.dao

import androidx.room.*
import com.dmb.joblog.data.local.entity.JobOfferEntity
import kotlinx.coroutines.flow.Flow

@Dao
internal interface JobOfferDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(offer: JobOfferEntity): Long

    @Update
    suspend fun update(offer: JobOfferEntity)

    @Delete
    suspend fun delete(offer: JobOfferEntity)

    @Query("DELETE FROM job_offers")
    suspend fun deleteAll()

    @Query("SELECT * FROM job_offers ORDER BY createdAtEpochMillis DESC")
    fun getAll(): Flow<List<JobOfferEntity>>

    @Query("SELECT * FROM job_offers WHERE status = :status ORDER BY createdAtEpochMillis DESC")
    fun getByStatus(status: String): Flow<List<JobOfferEntity>>

    @Query("SELECT * FROM job_offers WHERE id = :id")
    suspend fun getById(id: Long): JobOfferEntity?
}