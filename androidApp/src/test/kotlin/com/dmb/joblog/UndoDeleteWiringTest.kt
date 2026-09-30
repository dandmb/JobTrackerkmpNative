package com.dmb.joblog

import java.io.File
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class UndoDeleteWiringTest {

    private val source = listOf(
        "src/main/kotlin/com/dmb/joblog/ui/joboffer/JobOfferListScreen.kt",
        "androidApp/src/main/kotlin/com/dmb/joblog/ui/joboffer/JobOfferListScreen.kt",
    ).map(::File).first { it.exists() }.readText()

    private val undoBlock = source.substringAfter("SnackbarResult.ActionPerformed").substringBefore("restoredOfferId = id")

    @Test
    fun snackbarUndo_restoresTheRememberedDeletionById() {
        assertTrue(undoBlock.contains("viewModel.onRestoreDeletedOffer(id)"))
    }

    @Test
    fun snackbarUndo_doesNotReAddTheOfferWithOnAddOffer() {
        assertFalse(undoBlock.lines().filterNot { it.trim().startsWith("//") }.any { it.contains("onAddOffer(") },
            "« Annuler » ne doit pas ré-ajouter l'offre (createdAt = maintenant → remonte en tête)")
    }

    @Test
    fun snackbarUndo_stillScrollsToTheRestoredCard() {
        assertTrue(source.contains("restoredOfferId = id"))
    }

    @Test
    fun pendingUndo_survivesARotation_andTheSnackbarIsShownAgain() {
        assertTrue(source.contains("var pendingUndoOfferId by rememberSaveable"), "l'offre en attente d'« Annuler » doit être sauvegardée")
        assertTrue(source.contains("var pendingUndoTitle by rememberSaveable"))
        assertTrue(source.contains("LaunchedEffect(pendingUndoOfferId)"), "le snackbar doit être relancé depuis l'état sauvegardé")
        assertFalse(source.substringAfter("onDelete = {").substringBefore("onStatusChanged").contains("showSnackbar"),
            "un snackbar lancé depuis le geste est annulé par la rotation")
    }
}
