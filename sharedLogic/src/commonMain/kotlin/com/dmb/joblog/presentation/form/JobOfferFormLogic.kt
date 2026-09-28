package com.dmb.joblog.presentation.form

import com.dmb.joblog.domain.model.ApplicationStatus
import com.dmb.joblog.domain.model.JobOffer
import com.dmb.joblog.i18n.AppLanguage
import com.dmb.joblog.i18n.pick
import kotlinx.datetime.LocalDate

/** Les deux champs numériques (en k€) du formulaire. Chaîne vide = champ non rempli. */
data class SalaryFields(val min: String, val max: String)

/** Valeurs saisies dans le formulaire, avant conversion en [JobOffer]. */
data class JobOfferFormDraft(
    val title: String,
    val company: String,
    val url: String,
    val location: String,
    val source: String,
    val salaryMin: String,
    val salaryMax: String,
    val appliedDate: LocalDate,
    val interviewDate: LocalDate?,
    val resultDate: LocalDate?,
    val notes: String,
)

/**
 * Résultat de la validation du formulaire. Le bouton Enregistrer n'est actif que pour [Valid].
 * `isValid` / `errorMessage` évitent aux écrans (Compose, SwiftUI) de tester des sous-classes.
 */
sealed class FormValidation {
    /** Le formulaire peut être enregistré. */
    data object Valid : FormValidation()

    /** Titre ou entreprise vide : bouton désactivé SANS message (un champ vide se voit ; comportement historique). */
    data object MissingRequiredField : FormValidation()

    /** Le salaire est incohérent (max sans min) : bouton désactivé ET [message] à afficher à l'utilisateur. */
    data class InvalidSalary(val message: String) : FormValidation()

    val isValid: Boolean get() = this is Valid

    /** Message à afficher à l'utilisateur, ou `null` (formulaire valide, ou champ obligatoire vide sans message). */
    val errorMessage: String? get() = (this as? InvalidSalary)?.message
}

/**
 * Logique du formulaire d'ajout / édition, UNIQUE pour Android et iOS (les deux écrans ne font qu'appeler ces fonctions).
 *
 * Règles de référence (décisions produit, voir PROJECT_CONTEXT.md) :
 *  1. Un champ texte optionnel vide OU blanc devient `null` (jamais une chaîne d'espaces).
 *  2. Titre et entreprise : « vides » = `isBlank()` (espaces, tabulations, retours à la ligne) ; refusés.
 *  3. Salaire, saisie : 4 caractères au plus, chiffres uniquement ; une saisie trop longue est ignorée en bloc.
 *  4. Salaire, enregistrement : deux formes seulement, sinon `null` :  min + max → « 55k - 70k »  ·  min seul → « 55k+ ».
 *  5. Salaire, chargement : un texte libre hérité est lu selon une grammaire explicite (voir [parseSalaryFields]) ;
 *     un texte ambigu est REJETÉ (champs vides) plutôt que deviné.
 *  6. Un salaire MAX sans MIN est refusé à l'enregistrement, avec un message ([salaryMinRequiredMessage]) : l'écran
 *     désactive Enregistrer et affiche le message tant que le min est vide alors que le max est rempli. Voir [validate].
 */
object JobOfferFormLogic {

    const val SALARY_INPUT_MAX_LENGTH = 4

    /** Message affiché quand le salaire max est rempli sans salaire min (règle 6), dans la langue de l'interface. */
    fun salaryMinRequiredMessage(language: AppLanguage): String = language.pick(
        en = "Please also enter the minimum salary, or leave both fields empty.",
        fr = "Renseigne aussi le salaire minimum, ou laisse les deux champs vides.",
    )

    /**
     * Chaîne stockée → champs du formulaire (chargement d'une offre en édition).
     *  - `null` / blanc                        → champs vides ;
     *  - commence par « jusqu'à »              → max seul (« jusqu'à 70k » → ["", "70"]) : format que le formulaire
     *                                            n'écrit PLUS (règle 6) mais qu'on continue de LIRE, voir ci-dessous ;
     *  - aucun tiret                           → min seul (« 55k+ », « 70k » → ["55", ""]) ;
     *  - exactement UN tiret                   → chiffres à gauche = min, à droite = max (« 55k - 70k », « -70k » → ["", "70"],
     *                                            « 55k - » → ["55", ""], « 55000-70000 € » → ["55000", "70000"]) ;
     *  - deux tirets ou plus (« 55k - 70k - 80k ») → ambigu : REJETÉ, champs vides (on n'invente pas de valeur).
     * Pourquoi lire encore « jusqu'à » : il n'a jamais été écrit en production, mais a pu l'être par une build de test. Sans ce cas,
     * le texte (sans tiret) serait lu comme « min = 70 », soit un changement de sens silencieux (« au moins 70k » au lieu de
     * « jusqu'à 70k »). Lu comme max seul, il ouvre le formulaire avec le message de la règle 6 : rien n'est perdu ni réinterprété.
     * Seuls les chiffres de chaque côté sont gardés : « 45.5k » donne "455" (limite connue ; le formulaire n'écrit que des entiers).
     */
    fun parseSalaryFields(salaryRange: String?): SalaryFields {
        val text = salaryRange?.trim().orEmpty()
        return when {
            text.isEmpty() -> SalaryFields("", "")
            text.startsWith("jusqu", ignoreCase = true) -> SalaryFields("", digitsOf(text))
            else -> when (text.count { it == '-' }) {
                0 -> SalaryFields(digitsOf(text), "")
                1 -> SalaryFields(digitsOf(text.substringBefore('-')), digitsOf(text.substringAfter('-')))
                else -> SalaryFields("", "")
            }
        }
    }

    /**
     * Champs du formulaire → chaîne stockée (règle 4). Les caractères non numériques éventuels sont ignorés.
     * Un MAX sans MIN produit `null` : ce cas est refusé en amont par [validate] et ne doit pas atteindre l'enregistrement.
     */
    fun composeSalaryRange(min: String, max: String): String? {
        val low = digitsOf(min)
        val high = digitsOf(max)
        return when {
            low.isNotEmpty() && high.isNotEmpty() -> "${low}k - ${high}k"
            low.isNotEmpty() -> "${low}k+"
            else -> null
        }
    }

    /**
     * Filtre de saisie d'un champ de salaire (règle 3) : au plus [SALARY_INPUT_MAX_LENGTH] caractères saisis, seuls les
     * chiffres sont gardés. Une saisie plus longue est IGNORÉE en bloc (on garde la valeur précédente), non tronquée.
     */
    fun nextSalaryInput(previous: String, input: String): String =
        if (input.length <= SALARY_INPUT_MAX_LENGTH) digitsOf(input) else previous

    /**
     * Valide le formulaire (règles 2 et 6). Le bouton Enregistrer/Ajouter n'est actif que pour [FormValidation.Valid].
     * Ordre : le salaire d'abord, pour que son message (le seul à afficher) apparaisse dès que le max est rempli sans min,
     * même si le titre est encore vide ; puis les champs obligatoires.
     */
    fun validate(title: String, company: String, salaryMin: String, salaryMax: String, language: AppLanguage): FormValidation = when {
        digitsOf(salaryMin).isEmpty() && digitsOf(salaryMax).isNotEmpty() ->
            FormValidation.InvalidSalary(salaryMinRequiredMessage(language))
        title.isBlank() || company.isBlank() -> FormValidation.MissingRequiredField
        else -> FormValidation.Valid
    }

    /** Date de candidature au chargement : celle de l'offre en édition, sinon aujourd'hui. */
    fun initialAppliedDate(existing: JobOffer?, today: LocalDate): LocalDate = existing?.appliedDate ?: today

    /**
     * Assemble l'offre à enregistrer. Champs optionnels blancs → `null` (règle 1) ; titre et entreprise sont transmis
     * tels quels (non nettoyés). En édition l'id et le statut d'origine sont conservés ; à l'ajout : id 0, statut « Postulé ».
     */
    fun toJobOffer(draft: JobOfferFormDraft, existing: JobOffer?): JobOffer = JobOffer(
        id = existing?.id ?: 0,
        title = draft.title,
        company = draft.company,
        url = draft.url.ifBlank { null },
        location = draft.location.ifBlank { null },
        source = draft.source.ifBlank { null },
        salaryRange = composeSalaryRange(draft.salaryMin, draft.salaryMax),
        appliedDate = draft.appliedDate,
        interviewDate = draft.interviewDate,
        resultDate = draft.resultDate,
        status = existing?.status ?: ApplicationStatus.APPLIED,
        notes = draft.notes.ifBlank { null },
    )

    private fun digitsOf(text: String): String = text.filter { it.isDigit() }
}
