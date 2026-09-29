package com.dmb.joblog.presentation.form

import com.dmb.joblog.domain.model.ApplicationStatus
import com.dmb.joblog.domain.model.JobOffer
import com.dmb.joblog.i18n.AppLanguage
import com.dmb.joblog.i18n.pick
import kotlinx.datetime.LocalDate

data class SalaryFields(val min: String, val max: String)

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
    val cvAttachmentId: Long? = null,
    val coverLetterAttachmentId: Long? = null,
)

sealed class FormValidation {
    data object Valid : FormValidation()

    data object MissingRequiredField : FormValidation()

    data class InvalidSalary(val message: String) : FormValidation()

    val isValid: Boolean get() = this is Valid

    val errorMessage: String? get() = (this as? InvalidSalary)?.message
}

object JobOfferFormLogic {

    const val SALARY_INPUT_MAX_LENGTH = 4

    fun salaryMinRequiredMessage(language: AppLanguage): String = language.pick(
        en = "Please also enter the minimum salary, or leave both fields empty.",
        fr = "Renseigne aussi le salaire minimum, ou laisse les deux champs vides.",
    )

    fun parseSalaryFields(salaryRange: String?): SalaryFields {
        val text = salaryRange?.trim().orEmpty()
        return when {
            text.isEmpty() -> SalaryFields("", "")
            // Format hérité (jamais écrit par ce formulaire, mais possible via une build de test) : sans ce cas, le texte
            // serait lu comme un salaire minimum — un changement de sens silencieux.
            text.startsWith("jusqu", ignoreCase = true) -> SalaryFields("", digitsOf(text))
            else -> when (text.count { it == '-' }) {
                0 -> SalaryFields(digitsOf(text), "")
                1 -> SalaryFields(digitsOf(text.substringBefore('-')), digitsOf(text.substringAfter('-')))
                else -> SalaryFields("", "")
            }
        }
    }

    fun composeSalaryRange(min: String, max: String): String? {
        val low = digitsOf(min)
        val high = digitsOf(max)
        return when {
            low.isNotEmpty() && high.isNotEmpty() -> "${low}k - ${high}k"
            low.isNotEmpty() -> "${low}k+"
            else -> null
        }
    }

    fun nextSalaryInput(previous: String, input: String): String =
        if (input.length <= SALARY_INPUT_MAX_LENGTH) digitsOf(input) else previous

    fun validate(title: String, company: String, salaryMin: String, salaryMax: String, language: AppLanguage): FormValidation = when {
        digitsOf(salaryMin).isEmpty() && digitsOf(salaryMax).isNotEmpty() ->
            FormValidation.InvalidSalary(salaryMinRequiredMessage(language))
        title.isBlank() || company.isBlank() -> FormValidation.MissingRequiredField
        else -> FormValidation.Valid
    }

    fun initialAppliedDate(existing: JobOffer?, today: LocalDate): LocalDate = existing?.appliedDate ?: today

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
        cvAttachmentId = draft.cvAttachmentId,
        coverLetterAttachmentId = draft.coverLetterAttachmentId,
    )

    private fun digitsOf(text: String): String = text.filter { it.isDigit() }
}
