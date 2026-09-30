package com.dmb.joblog

import java.io.File
import kotlin.test.Test
import kotlin.test.assertTrue

class CardEditLabelWiringTest {

    private val card = listOf(
        "src/main/kotlin/com/dmb/joblog/ui/joboffer/JobOfferCard.kt",
        "androidApp/src/main/kotlin/com/dmb/joblog/ui/joboffer/JobOfferCard.kt",
    ).map(::File).first { it.exists() }.readText()

    @Test
    fun editButtonLabel_namesTheOfferAsTheCardDisplaysIt() {
        assertTrue(card.contains("stringResource(R.string.card_edit_a11y, offer.title.toTitleCase())"),
            "le lecteur d'écran doit annoncer le titre tel qu'il est affiché")
    }
}
