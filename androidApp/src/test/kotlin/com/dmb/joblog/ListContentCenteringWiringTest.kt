package com.dmb.joblog

import java.io.File
import kotlin.test.Test
import kotlin.test.assertTrue

class ListContentCenteringWiringTest {

    private val source = listOf(
        "src/main/kotlin/com/dmb/joblog/ui/joboffer/JobOfferListScreen.kt",
        "androidApp/src/main/kotlin/com/dmb/joblog/ui/joboffer/JobOfferListScreen.kt",
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
        val text = source.substringAfter("R.string.list_no_results").substringBefore("items(visibleOffers")
        assertTrue(text.contains("fillMaxWidth()") && text.contains("TextAlign.Center"),
            "le texte « Aucun résultat » doit occuper la largeur et être centré (TextAlign.Center)")
    }
}
