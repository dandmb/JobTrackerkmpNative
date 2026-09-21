package com.dmb.jobtracker.domain.model

/**
 * Ce qu'il faut garder d'une candidature supprimée pour pouvoir l'ANNULER fidèlement : l'offre elle-même ET son horodatage
 * de création d'origine. Le modèle [JobOffer] n'expose pas `createdAt` (donnée technique servant au tri de la liste) : sans
 * ce complément, un « Annuler » ré-insérait l'offre avec `createdAt = maintenant` et la faisait remonter en tête de liste.
 *
 * `createdAtEpochMillis` est nul si l'offre n'existait déjà plus au moment de la suppression.
 */
data class DeletedJobOffer(
    val offer: JobOffer,
    val createdAtEpochMillis: Long?,
)
