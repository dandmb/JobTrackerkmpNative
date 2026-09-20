package com.dmb.jobtracker.testutil

import com.dmb.jobtracker.data.local.entity.JobOfferEntity
import com.dmb.jobtracker.domain.model.ApplicationStatus
import com.dmb.jobtracker.domain.model.JobOffer
import kotlinx.datetime.LocalDate

/** Fabrique un [JobOffer] valide ; on ne surcharge que ce que le test veut observer. */
fun jobOffer(
    id: Long = 0,
    title: String = "Développeur Kotlin",
    company: String = "Acme",
    url: String? = null,
    location: String? = null,
    source: String? = null,
    salaryRange: String? = null,
    appliedDate: LocalDate = LocalDate(2026, 9, 5),
    interviewDate: LocalDate? = null,
    resultDate: LocalDate? = null,
    status: ApplicationStatus = ApplicationStatus.APPLIED,
    notes: String? = null,
) = JobOffer(
    id = id,
    title = title,
    company = company,
    url = url,
    location = location,
    source = source,
    salaryRange = salaryRange,
    appliedDate = appliedDate,
    interviewDate = interviewDate,
    resultDate = resultDate,
    status = status,
    notes = notes,
)

/** Fabrique un [JobOfferEntity] valide (dates en jours depuis l'époque : 2026-09-05 = 20701). */
fun jobOfferEntity(
    id: Long = 0,
    title: String = "Développeur Kotlin",
    company: String = "Acme",
    url: String? = null,
    location: String? = null,
    source: String? = null,
    salaryRange: String? = null,
    appliedDateEpochDays: Long = 20_701,
    interviewDateEpochDays: Long? = null,
    resultDateEpochDays: Long? = null,
    status: ApplicationStatus = ApplicationStatus.APPLIED,
    notes: String? = null,
    createdAtEpochMillis: Long = 1_000L,
) = JobOfferEntity(
    id = id,
    title = title,
    company = company,
    url = url,
    location = location,
    source = source,
    salaryRange = salaryRange,
    appliedDateEpochDays = appliedDateEpochDays,
    interviewDateEpochDays = interviewDateEpochDays,
    resultDateEpochDays = resultDateEpochDays,
    status = status,
    notes = notes,
    createdAtEpochMillis = createdAtEpochMillis,
)
