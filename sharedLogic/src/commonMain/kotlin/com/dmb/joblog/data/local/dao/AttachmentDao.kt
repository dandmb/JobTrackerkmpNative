package com.dmb.joblog.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.dmb.joblog.data.local.entity.AttachmentEntity
import com.dmb.joblog.data.local.entity.AttachmentWithUsage
import kotlinx.coroutines.flow.Flow

@Dao
internal interface AttachmentDao {

    @Insert
    suspend fun insert(attachment: AttachmentEntity): Long

    @Query(
        "SELECT attachments.*, (SELECT COUNT(*) FROM job_offer_attachments WHERE job_offer_attachments.attachmentId = attachments.id) " +
            "AS usageCount FROM attachments ORDER BY addedAtEpochMillis DESC, id DESC"
    )
    fun getAllWithUsage(): Flow<List<AttachmentWithUsage>>

    @Query("SELECT * FROM attachments WHERE id = :id")
    suspend fun getById(id: Long): AttachmentEntity?

    @Query("SELECT COUNT(*) FROM job_offer_attachments WHERE attachmentId = :id")
    suspend fun usageCount(id: Long): Int

    @Query("UPDATE attachments SET displayName = :displayName WHERE id = :id")
    suspend fun rename(id: Long, displayName: String)

    @Query("DELETE FROM attachments WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("SELECT * FROM attachments WHERE inLibrary = 0 AND id NOT IN (SELECT attachmentId FROM job_offer_attachments)")
    suspend fun getOrphanOneTimeAttachments(): List<AttachmentEntity>

    @Query("SELECT storageName FROM attachments")
    suspend fun getAllStorageNames(): List<String>
}
