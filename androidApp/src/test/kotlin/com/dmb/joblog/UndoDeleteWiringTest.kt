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

    private val undoBlock = source.substringAfter("SnackbarResult.ActionPerformed").substringBefore("restoredOfferId = offer.id")

    @Test
    fun snackbarUndo_restoresThroughOnRestoreOffer() {
        assertTrue(undoBlock.contains("viewModel.onRestoreOffer(offer)"))
    }

    @Test
    fun snackbarUndo_doesNotReAddTheOfferWithOnAddOffer() {
        assertFalse(undoBlock.lines().filterNot { it.trim().startsWith("//") }.any { it.contains("onAddOffer(") },
            "« Annuler » ne doit pas ré-ajouter l'offre (createdAt = maintenant → remonte en tête)")
    }

    @Test
    fun snackbarUndo_stillScrollsToTheRestoredCard() {
        assertTrue(source.contains("restoredOfferId = offer.id"))
    }
}
