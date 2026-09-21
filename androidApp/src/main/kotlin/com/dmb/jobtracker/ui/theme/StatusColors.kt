package com.dmb.jobtracker.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import com.dmb.jobtracker.domain.model.ApplicationStatus

/** Couleurs de statut du mode courant (fournies par [JobTrackerTheme]). */
@Immutable
class StatusPalette(
    val pending: Color,
    val applied: Color,
    val interview: Color,
    val rejected: Color,
    val accepted: Color,
    val badgeTintAlpha: Float,
)

val LightStatusPalette = StatusPalette(
    pending = StatusPendingLight,
    applied = StatusAppliedLight,
    interview = StatusInterviewLight,
    rejected = StatusRejectedLight,
    accepted = StatusAcceptedLight,
    badgeTintAlpha = StatusBadgeTintLight,
)

val DarkStatusPalette = StatusPalette(
    pending = StatusPendingDark,
    applied = StatusAppliedDark,
    interview = StatusInterviewDark,
    rejected = StatusRejectedDark,
    accepted = StatusAcceptedDark,
    badgeTintAlpha = StatusBadgeTintDark,
)

val LocalStatusPalette = staticCompositionLocalOf { LightStatusPalette }

@Composable
fun ApplicationStatus.color(): Color {
    val palette = LocalStatusPalette.current
    return when (this) {
        ApplicationStatus.PENDING -> palette.pending
        ApplicationStatus.APPLIED -> palette.applied
        ApplicationStatus.INTERVIEW -> palette.interview
        ApplicationStatus.REJECTED -> palette.rejected
        ApplicationStatus.ACCEPTED -> palette.accepted
    }
}
