package com.dmb.joblog.presentation.onboarding

import com.dmb.joblog.i18n.AppLanguage
import com.dmb.joblog.i18n.pick

/** Une page de l'onboarding. L'icône est choisie par chaque plateforme (Material / SF Symbols) selon l'index. */
data class OnboardingPage(val title: String, val description: String)

/**
 * Contenu et règles de navigation de l'onboarding, COMMUNS à Android et iOS : mêmes textes, mêmes boutons, mêmes
 * conditions d'affichage (« Passer » sur les pages non finales ; « Suivant » puis « Commencer » sur la dernière),
 * dans la langue demandée ([AppLanguage]).
 *
 * Les noms de statut cités dans la 2e page doivent rester identiques aux libellés de statut des écrans
 * (`status_*` dans `strings.xml` / `Localizable.strings`) : un test le vérifie.
 */
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

    /** « Passer » n'est proposé que tant qu'il reste des pages à voir. */
    fun showsSkip(index: Int): Boolean = !isLastPage(index)

    fun primaryButtonLabel(index: Int): String = if (isLastPage(index)) startLabel else nextLabel

    /** Index de la page suivante, borné à la dernière page (le bouton principal de la dernière page termine l'onboarding). */
    fun nextPageIndex(index: Int): Int = (index + 1).coerceIn(0, pages.lastIndex)

    companion object {
        fun of(language: AppLanguage): OnboardingContent = OnboardingContent(language)
    }
}
