package com.dmb.joblog.data.local.entity

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "attachments", indices = [Index(value = ["storageName"], unique = true)])
data class AttachmentEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val displayName: String,
    val kind: String,
    val format: String,
    val sizeBytes: Long,
    val addedAtEpochMillis: Long,
    val inLibrary: Boolean,
    val storageName: String,
)

data class AttachmentWithUsage(
    @Embedded val attachment: AttachmentEntity,
    val usageCount: Int,
)
