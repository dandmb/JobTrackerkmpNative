package com.dmb.jobtracker.ui.joboffer

import java.io.File
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Garde-fou d'architecture : l'écran Android n'a PAS le droit de réimplémenter la logique de formulaire, il doit appeler
 * la logique commune (`sharedLogic` → `JobOfferFormLogic`). Un vrai test de câblage demanderait un test UI Compose ;
 * ce contrôle lit donc le source (approche volontairement simple, un peu fragile si le fichier est renommé).
 */
class FormSheetUsesSharedLogicTest {

    private val source: String = listOf(
        "src/main/kotlin/com/dmb/jobtracker/ui/joboffer/JobOfferFormSheet.kt",
        "androidApp/src/main/kotlin/com/dmb/jobtracker/ui/joboffer/JobOfferFormSheet.kt",
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
        // Câblage de la règle « salaire max sans min » : bouton lié à la validation partagée, message affiché.
        assertTrue(source.contains("enabled = validation.isValid"), "le bouton Enregistrer doit dépendre de validation.isValid")
        assertTrue(source.contains("validation.errorMessage"), "le message d'erreur de validation doit être affiché")
    }

    @Test
    fun formSheet_importsTheSharedLogicFromSharedLogicModule() {
        assertTrue(source.contains("import com.dmb.jobtracker.presentation.form.JobOfferFormLogic"))
        assertTrue(source.contains("import com.dmb.jobtracker.presentation.form.JobOfferFormDraft"))
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
