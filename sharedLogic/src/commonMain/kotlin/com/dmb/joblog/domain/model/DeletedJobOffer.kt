package com.dmb.joblog.domain.model

data class DeletedJobOffer(
    val offer: JobOffer,
    val createdAtEpochMillis: Long?,
)
