package com.dmb.joblog.presentation.joboffer

import com.dmb.joblog.domain.model.JobOffer

data class JobOfferListState(
    val offers: List<JobOffer> = emptyList(),
    val isLoading: Boolean = true,
    val errorMessage: String? = null
)