package com.dmb.joblog.data.local.dao

import androidx.room.*
import com.dmb.joblog.data.local.entity.JobOfferAttachmentEntity
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

    @Query("SELECT * FROM job_offer_attachments")
    fun getAllAttachmentLinks(): Flow<List<JobOfferAttachmentEntity>>

    @Query("SELECT * FROM job_offer_attachments WHERE jobOfferId = :jobOfferId")
    suspend fun getAttachmentLinks(jobOfferId: Long): List<JobOfferAttachmentEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAttachmentLink(link: JobOfferAttachmentEntity)

    @Query("DELETE FROM job_offer_attachments WHERE jobOfferId = :jobOfferId AND role = :role")
    suspend fun deleteAttachmentLink(jobOfferId: Long, role: String)

    @Query("SELECT COUNT(*) FROM attachments WHERE id = :attachmentId")
    suspend fun attachmentExists(attachmentId: Long): Int

    @Query("DELETE FROM attachments")
    suspend fun deleteAllAttachments()
}