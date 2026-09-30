package com.dmb.joblog

import java.io.File
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class StateSurvivesRotationWiringTest {

    private fun read(relative: String): String = listOf(relative, "androidApp/$relative").map(::File).first { it.exists() }.readText()

    private val ui = "src/main/kotlin/com/dmb/joblog/ui"
    private val list = read("$ui/joboffer/JobOfferListScreen.kt")
    private val form = read("$ui/joboffer/JobOfferFormSheet.kt")
    private val documents = read("$ui/attachments/DocumentsScreen.kt")
    private val formSection = read("$ui/attachments/DocumentsFormSection.kt")

    @Test
    fun listScreen_savesTheOpenSheetTheSearchTheSortAndTheFilter() {
        assertTrue(list.contains("var showAddSheet by rememberSaveable"))
        assertTrue(list.contains("var editedOfferId by rememberSaveable"), "l'offre en édition est retrouvée par son id après rotation")
        assertTrue(list.contains("var searchQuery by rememberSaveable"))
        assertTrue(list.contains("var sortOption by rememberSaveable"))
        assertTrue(list.contains("var selectedStatuses by rememberSaveable(stateSaver = StatusSetSaver)"))
    }

    @Test
    fun formSheet_savesEveryField() {
        for (field in listOf("title", "company", "url", "location", "source", "notes", "salaryMin", "salaryMax",
            "cvAttachmentId", "coverLetterAttachmentId")) {
            assertTrue(form.contains("var $field by rememberSaveable"), "champ « $field » perdu à la rotation")
        }
        assertTrue(form.contains("var appliedDate by rememberSaveable(stateSaver = LocalDateSaver)"))
        assertTrue(form.contains("var interviewDate by rememberSaveable(stateSaver = OptionalLocalDateSaver)"))
        assertTrue(form.contains("var resultDate by rememberSaveable(stateSaver = OptionalLocalDateSaver)"))
        assertFalse(form.contains("by remember { mutableStateOf(existingOffer"), "plus aucun champ en simple remember")
    }

    @Test
    fun pendingImportChoices_surviveTheSystemPickerAndARotation() {
        assertTrue(documents.contains("var pendingKind by rememberSaveable"), "sinon le fichier choisi après rotation est ignoré")
        assertTrue(formSection.contains("var importInLibrary by rememberSaveable"))
    }
}
