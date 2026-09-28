package com.dmb.joblog.ui.joboffer

import java.io.File
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class FormSheetUsesSharedLogicTest {

    private val source: String = listOf(
        "src/main/kotlin/com/dmb/joblog/ui/joboffer/JobOfferFormSheet.kt",
        "androidApp/src/main/kotlin/com/dmb/joblog/ui/joboffer/JobOfferFormSheet.kt",
    ).map(::File).first { it.exists() }.readText()

    @Test
    fun formSheet_callsTheSharedFormLogicForEveryRule() {
        listOf(
            "JobOfferFormLogic.initialAppliedDate(",
            "JobOfferFormLogic.parseSalaryFields(",
            "JobOfferFormLogic.nextSalaryInput(",
            "JobOfferFormLogic.validate(",
            "JobOfferFormLogic.toJobOffer(",
        ).forEach { call -> assertTrue(source.contains(call), "JobOfferFormSheet doit appeler $call") }
    }

    @Test
    fun formSheet_disablesSaveOnValidationAndShowsTheSharedErrorMessage() {
        assertTrue(source.contains("enabled = validation.isValid"), "le bouton Enregistrer doit dépendre de validation.isValid")
        assertTrue(source.contains("validation.errorMessage"), "le message d'erreur de validation doit être affiché")
    }

    @Test
    fun formSheet_importsTheSharedLogicFromSharedLogicModule() {
        assertTrue(source.contains("import com.dmb.joblog.presentation.form.JobOfferFormLogic"))
        assertTrue(source.contains("import com.dmb.joblog.presentation.form.JobOfferFormDraft"))
    }

    @Test
    fun formSheet_doesNotReimplementParsingOrValidationRules() {
        listOf(
            "substringBefore(\"-\")",
            "substringAfter(\"-\"",
            "ifBlank { null }",
            ".isNotBlank() && company.isNotBlank()",
            "salaryMax.isNotBlank()",
            "filter { c -> c.isDigit() }",
            "\"k - \"",
            "jusqu'à",
            "isFormSavable",
        ).forEach { forbidden ->
            assertFalse(source.contains(forbidden), "JobOfferFormSheet ne doit plus contenir la règle « $forbidden » (déplacée dans sharedLogic)")
        }
    }
}
