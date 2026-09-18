package com.dmb.jobtracker.data.mapper

import com.dmb.jobtracker.data.local.entity.JobOfferEntity
import com.dmb.jobtracker.domain.model.JobOffer
import kotlinx.datetime.LocalDate

fun JobOfferEntity.toDomain(): JobOffer = JobOffer(
    id = id,
    title = title,
    company = company,
    url = url,
    appliedDate = LocalDate.fromEpochDays(appliedDateEpochDays.toInt()),
    status = status,
    notes = notes
)

fun JobOffer.toEntity(): JobOfferEntity = JobOfferEntity(
    id = id,
    title = title,
    company = company,
    url = url,
    appliedDateEpochDays = appliedDate.toEpochDays(),
    status = status,
    notes = notes,
    createdAtEpochMillis = kotlin.time.Clock.System.now().toEpochMilliseconds()
)