package com.dmb.jobtracker

import java.io.File
import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * Garde-fou (lecture du source) : le conteneur du contenu de la liste doit occuper toute la largeur, sinon les enfants
 * alignés `Alignment.Center` (« Aucun résultat pour… », indicateur de chargement) sont centrés dans un Box aussi étroit que
 * leur contenu, donc collés au bord gauche (défaut constaté sur émulateur, corrigé).
 */
class ListContentCenteringWiringTest {

    private val source = listOf(
        "src/main/kotlin/com/dmb/jobtracker/ui/joboffer/JobOfferListScreen.kt",
        "androidApp/src/main/kotlin/com/dmb/jobtracker/ui/joboffer/JobOfferListScreen.kt",
    ).map(::File).first { it.exists() }.readText()

    @Test
    fun contentBox_withWeight_alsoFillsTheWidth() {
        assertTrue(source.contains("Box(modifier = Modifier.weight(1f).fillMaxWidth())"),
            "le Box du contenu doit être `weight(1f).fillMaxWidth()` (centrage des états « aucun résultat » / chargement)")
    }

    @Test
    fun loadingIndicator_isInsideThatBox() {
        val box = source.indexOf("Box(modifier = Modifier.weight(1f).fillMaxWidth())")
        val loading = source.indexOf("CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))")

        assertTrue(box in 0 until loading, "l'indicateur de chargement centré doit être dans ce Box")
    }

    @Test
    fun noResultText_isCenteredHorizontallyByItself() {
        val text = source.substringAfter("\"Aucun résultat pour « \$searchQuery »\"").substringBefore("items(visibleOffers")
        assertTrue(text.contains("fillMaxWidth()") && text.contains("TextAlign.Center"),
            "le texte « Aucun résultat » doit occuper la largeur et être centré (TextAlign.Center)")
    }
}
