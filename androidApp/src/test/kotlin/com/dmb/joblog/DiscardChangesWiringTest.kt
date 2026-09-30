package com.dmb.joblog

import java.io.File
import kotlin.test.Test
import kotlin.test.assertTrue

class DiscardChangesWiringTest {

    private val form = listOf(
        "src/main/kotlin/com/dmb/joblog/ui/joboffer/JobOfferFormSheet.kt",
        "androidApp/src/main/kotlin/com/dmb/joblog/ui/joboffer/JobOfferFormSheet.kt",
    ).map(::File).first { it.exists() }.readText()

    @Test
    fun unsavedChanges_areDecidedByTheSharedFormLogic() {
        assertTrue(form.contains("JobOfferFormLogic.hasUnsavedChanges("), "la règle « modifié ? » ne doit pas être réimplémentée dans l'écran")
    }

    @Test
    fun hidingAModifiedSheet_isRefused_andAsksForConfirmation() {
        assertTrue(form.contains("confirmValueChange = { value ->"), "le glissement, le fond et le retour arrière passent tous par confirmValueChange")
        assertTrue(form.contains("value != SheetValue.Hidden || !currentHasUnsavedChanges"))
        assertTrue(form.contains("if (!allowed) showDiscardConfirmation = true"))
        assertTrue(form.contains("rememberUpdatedState(hasUnsavedChanges)"), "le lambda est créé une fois : il doit lire l'état à jour")
    }

    @Test
    fun discardConfirmation_usesTheSharedTexts_andOnlyTheConfirmButtonCloses() {
        for (key in listOf("form_discard_title", "form_discard_message", "form_discard_confirm", "form_discard_cancel")) {
            assertTrue(form.contains("R.string.$key"), key)
        }
        val confirm = form.substringAfter("R.string.form_discard_confirm").substringBefore("dismissButton")
        val dialog = form.substringAfter("if (showDiscardConfirmation)").substringBefore("R.string.form_discard_confirm")
        assertTrue(dialog.contains("onDismiss()"), "« Abandonner » ferme le formulaire")
        assertTrue(!confirm.contains("onDismiss()"))
    }
}
