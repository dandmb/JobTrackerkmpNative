package com.dmb.jobtracker.ui.joboffer

import com.dmb.jobtracker.domain.model.ApplicationStatus

/** Libellé complet (chip de la carte, menu de statut). */
fun ApplicationStatus.displayLabel(): String = when (this) {
    ApplicationStatus.PENDING -> "En attente"
    ApplicationStatus.APPLIED -> "Postulé"
    ApplicationStatus.INTERVIEW -> "Entretien"
    ApplicationStatus.REJECTED -> "Refusé"
    ApplicationStatus.ACCEPTED -> "Accepté"
}

/** Libellé court en minuscules (badges de la carte de statistiques : « 2 postulé »). */
fun ApplicationStatus.shortLabel(): String = when (this) {
    ApplicationStatus.PENDING -> "attente"
    ApplicationStatus.APPLIED -> "postulé"
    ApplicationStatus.INTERVIEW -> "entretien"
    ApplicationStatus.REJECTED -> "refusé"
    ApplicationStatus.ACCEPTED -> "accepté"
}
