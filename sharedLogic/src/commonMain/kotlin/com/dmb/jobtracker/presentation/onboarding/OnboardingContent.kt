package com.dmb.jobtracker.presentation.onboarding

/** Une page de l'onboarding. L'icône est choisie par chaque plateforme (Material / SF Symbols) selon l'index. */
data class OnboardingPage(val title: String, val description: String)

/**
 * Contenu et règles de navigation de l'onboarding, COMMUNS à Android et iOS : mêmes textes, mêmes boutons, mêmes
 * conditions d'affichage (« Passer » sur les pages non finales ; « Suivant » puis « Commencer » sur la dernière).
 */
object OnboardingContent {

    const val SKIP_LABEL = "Passer"
    const val NEXT_LABEL = "Suivant"
    const val START_LABEL = "Commencer"

    val pages: List<OnboardingPage> = listOf(
        OnboardingPage(
            title = "Suis tes candidatures",
            description = "Ajoute chaque poste visé avec l'entreprise, le lieu, le salaire et les dates clés, au même endroit.",
        ),
        OnboardingPage(
            title = "Garde un œil sur les statuts",
            description = "Fais passer une candidature d'« En attente » à « Postulé », « Entretien », puis « Accepté » ou « Refusé » en un geste.",
        ),
        OnboardingPage(
            title = "Vois où tu en es",
            description = "Le résumé en haut de la liste compte tes candidatures par statut pour voir d'un coup d'œil ce qui avance.",
        ),
    )

    val pageCount: Int get() = pages.size

    fun isLastPage(index: Int): Boolean = index >= pages.lastIndex

    /** « Passer » n'est proposé que tant qu'il reste des pages à voir. */
    fun showsSkip(index: Int): Boolean = !isLastPage(index)

    fun primaryButtonLabel(index: Int): String = if (isLastPage(index)) START_LABEL else NEXT_LABEL

    /** Index de la page suivante, borné à la dernière page (le bouton principal de la dernière page termine l'onboarding). */
    fun nextPageIndex(index: Int): Int = (index + 1).coerceIn(0, pages.lastIndex)
}
