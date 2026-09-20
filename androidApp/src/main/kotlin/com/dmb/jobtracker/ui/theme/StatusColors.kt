package com.dmb.jobtracker.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.dmb.jobtracker.domain.model.ApplicationStatus

@Composable
fun ApplicationStatus.color(): Color = when (this) {
    ApplicationStatus.PENDING -> StatusPending
    ApplicationStatus.APPLIED -> StatusApplied
    ApplicationStatus.INTERVIEW -> StatusInterview
    ApplicationStatus.REJECTED -> StatusRejected
    ApplicationStatus.ACCEPTED -> StatusAccepted
}