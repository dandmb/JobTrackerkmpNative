package com.dmb.joblog.ui.joboffer

import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.dmb.joblog.R
import com.dmb.joblog.domain.model.ApplicationStatus

/** Ressource du libellé complet d'un statut (chip de la carte, menu de statut) : `status_*` dans strings.xml. */
@StringRes
fun ApplicationStatus.labelRes(): Int = when (this) {
    ApplicationStatus.PENDING -> R.string.status_pending
    ApplicationStatus.APPLIED -> R.string.status_applied
    ApplicationStatus.INTERVIEW -> R.string.status_interview
    ApplicationStatus.REJECTED -> R.string.status_rejected
    ApplicationStatus.ACCEPTED -> R.string.status_accepted
}

/**
 * Ressource du libellé court AVEC le nombre (badges de la carte de statistiques : « 2 applied » / « 2 postulé »).
 * Règle du pluriel : `count > 1` → `_other`, sinon `_one` (comme la carte « candidature(s) suivie(s) »).
 */
@StringRes
fun ApplicationStatus.shortLabelRes(count: Int): Int {
    val plural = count > 1
    return when (this) {
        ApplicationStatus.PENDING -> if (plural) R.string.stats_pending_other else R.string.stats_pending_one
        ApplicationStatus.APPLIED -> if (plural) R.string.stats_applied_other else R.string.stats_applied_one
        ApplicationStatus.INTERVIEW -> if (plural) R.string.stats_interview_other else R.string.stats_interview_one
        ApplicationStatus.REJECTED -> if (plural) R.string.stats_rejected_other else R.string.stats_rejected_one
        ApplicationStatus.ACCEPTED -> if (plural) R.string.stats_accepted_other else R.string.stats_accepted_one
    }
}

/** Libellé complet dans la langue courante. */
@Composable
fun ApplicationStatus.displayLabel(): String = stringResource(labelRes())

/** Libellé court avec nombre dans la langue courante (« 2 postulé »). */
@Composable
fun ApplicationStatus.shortLabel(count: Int): String = stringResource(shortLabelRes(count), count)
