package com.dmb.jobtracker.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.dmb.jobtracker.domain.model.ApplicationStatus

@Entity(tableName = "job_offers")
data class JobOfferEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val company: String,
    val url: String? = null,
    val location: String? = null,
    val source: String? = null,
    val salaryRange: String? = null,
    val appliedDateEpochDays: Long,
    val interviewDateEpochDays: Long? = null,
    val resultDateEpochDays: Long? = null,
    val status: ApplicationStatus,
    val notes: String? = null,
    val createdAtEpochMillis: Long
)