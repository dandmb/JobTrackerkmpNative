package com.dmb.joblog.data.mapper

import com.dmb.joblog.data.local.entity.JobOfferAttachmentEntity
import com.dmb.joblog.data.local.entity.JobOfferEntity
import com.dmb.joblog.domain.model.AttachmentKind
import com.dmb.joblog.domain.model.JobOffer
import kotlinx.datetime.LocalDate
import kotlinx.datetime.Clock

internal fun JobOfferEntity.toDomain(links: List<JobOfferAttachmentEntity> = emptyList()): JobOffer = JobOffer(
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
    notes = notes,
    cvAttachmentId = links.firstOrNull { it.role == AttachmentKind.CV.name }?.attachmentId,
    coverLetterAttachmentId = links.firstOrNull { it.role == AttachmentKind.COVER_LETTER.name }?.attachmentId,
)

internal fun JobOffer.attachmentIdsByRole(): Map<AttachmentKind, Long?> =
    mapOf(AttachmentKind.CV to cvAttachmentId, AttachmentKind.COVER_LETTER to coverLetterAttachmentId)

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