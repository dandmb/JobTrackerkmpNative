package com.dmb.jobtracker.data.mapper

import com.dmb.jobtracker.data.local.entity.JobOfferEntity
import com.dmb.jobtracker.domain.model.JobOffer
import kotlinx.datetime.LocalDate
import kotlinx.datetime.Clock

fun JobOfferEntity.toDomain(): JobOffer = JobOffer(
    id = id,
    title = title,
    company = company,
    url = url,
    location = location,
    source = source,
    salaryRange = salaryRange,
    appliedDate = LocalDate.fromEpochDays(appliedDateEpochDays.toInt()),
    interviewDate = interviewDateEpochDays?.let { LocalDate.fromEpochDays(it.toInt()) },
    resultDate = resultDateEpochDays?.let { LocalDate.fromEpochDays(it.toInt()) },
    status = status,
    notes = notes
)

fun JobOffer.toEntity(existingCreatedAt: Long? = null): JobOfferEntity = JobOfferEntity(
    id = id,
    title = title,
    company = company,
    url = url,
    location = location,
    source = source,
    salaryRange = salaryRange,
    appliedDateEpochDays = appliedDate.toEpochDays().toLong(),
    interviewDateEpochDays = interviewDate?.toEpochDays()?.toLong(),
    resultDateEpochDays = resultDate?.toEpochDays()?.toLong(),
    status = status,
    notes = notes,
    createdAtEpochMillis = existingCreatedAt ?: Clock.System.now().toEpochMilliseconds()
)