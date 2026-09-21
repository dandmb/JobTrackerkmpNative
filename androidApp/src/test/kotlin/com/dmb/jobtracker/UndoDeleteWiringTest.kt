package com.dmb.jobtracker

import java.io.File
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Garde-fou (lecture du source) : le « Annuler » du snackbar de suppression passe par `onRestoreOffer` (qui conserve
 * `createdAt` donc la position dans la liste) et non par `onAddOffer` (qui horodate à l'instant présent : la candidature
 * remontait en tête de liste). Défaut constaté et corrigé.
 */
class UndoDeleteWiringTest {

    private val source = listOf(
        "src/main/kotlin/com/dmb/jobtracker/ui/joboffer/JobOfferListScreen.kt",
        "androidApp/src/main/kotlin/com/dmb/jobtracker/ui/joboffer/JobOfferListScreen.kt",
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
