package com.dmb.joblog.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index

@Entity(
    tableName = "job_offer_attachments",
    primaryKeys = ["jobOfferId", "role"],
    foreignKeys = [
        ForeignKey(entity = JobOfferEntity::class, parentColumns = ["id"], childColumns = ["jobOfferId"], onDelete = ForeignKey.CASCADE),
        ForeignKey(entity = AttachmentEntity::class, parentColumns = ["id"], childColumns = ["attachmentId"], onDelete = ForeignKey.RESTRICT),
    ],
    indices = [Index(value = ["attachmentId"])],
)
data class JobOfferAttachmentEntity(
    val jobOfferId: Long,
    val role: String,
    val attachmentId: Long,
)
