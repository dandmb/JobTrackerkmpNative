package com.dmb.jobtracker.domain.model

import kotlinx.datetime.LocalDate

data class JobOffer(
    val id: Long = 0,
    val title: String,
    val company: String,
    val url: String? = null,
    val appliedDate: LocalDate,
    val status: ApplicationStatus,
    val notes: String? = null
)

enum class ApplicationStatus {
    PENDING, APPLIED, INTERVIEW, REJECTED, ACCEPTED
}