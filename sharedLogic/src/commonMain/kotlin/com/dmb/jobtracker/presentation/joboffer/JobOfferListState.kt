package com.dmb.jobtracker.presentation.joboffer

import com.dmb.jobtracker.domain.model.JobOffer

data class JobOfferListState(
    val offers: List<JobOffer> = emptyList(),
    val isLoading: Boolean = true,
    val errorMessage: String? = null
)