package com.dmb.joblog.presentation.onboarding

import com.dmb.joblog.i18n.AppLanguage
import com.dmb.joblog.i18n.pick

data class OnboardingPage(val title: String, val description: String)

/** Contenu et navigation de l'onboarding, communs à Android/iOS. Les statuts cités en page 2 doivent rester identiques aux libellés d'écran (test dédié). */
class OnboardingContent private constructor(private val lang: AppLanguage) {

    val skipLabel: String = lang.pick(en = "Skip", fr = "Passer")
    val nextLabel: String = lang.pick(en = "Next", fr = "Suivant")
    val startLabel: String = lang.pick(en = "Get started", fr = "Commencer")

    val pages: List<OnboardingPage> = listOf(
        OnboardingPage(
            title = lang.pick(en = "Track your applications", fr = "Suis tes candidatures"),
            description = lang.pick(
                en = "Add every job you're aiming for with the company, location, salary and key dates, all in one place.",
                fr = "Ajoute chaque poste visé avec l'entreprise, le lieu, le salaire et les dates clés, au même endroit.",
            ),
        ),
        OnboardingPage(
            title = lang.pick(en = "Keep an eye on statuses", fr = "Garde un œil sur les statuts"),
            description = lang.pick(
                en = "Move an application from “Pending” to “Applied”, “Interview”, then “Accepted” or “Rejected” in one gesture.",
                fr = "Fais passer une candidature d'« En attente » à « Postulé », « Entretien », puis « Accepté » ou « Refusé » en un geste.",
            ),
        ),
        OnboardingPage(
            title = lang.pick(en = "See where you stand", fr = "Vois où tu en es"),
            description = lang.pick(
                en = "The summary at the top of the list counts your applications by status so you can see at a glance what's moving.",
                fr = "Le résumé en haut de la liste compte tes candidatures par statut pour voir d'un coup d'œil ce qui avance.",
            ),
        ),
    )

    val pageCount: Int get() = pages.size

    fun isLastPage(index: Int): Boolean = index >= pages.lastIndex

    fun showsSkip(index: Int): Boolean = !isLastPage(index)

    fun primaryButtonLabel(index: Int): String = if (isLastPage(index)) startLabel else nextLabel

    fun nextPageIndex(index: Int): Int = (index + 1).coerceIn(0, pages.lastIndex)

    companion object {
        fun of(language: AppLanguage): OnboardingContent = OnboardingContent(language)
    }
}
