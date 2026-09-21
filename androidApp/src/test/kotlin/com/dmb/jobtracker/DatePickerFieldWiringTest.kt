package com.dmb.jobtracker

import java.io.File
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Garde-fous (lecture du source) du champ de date du formulaire. Défauts constatés sur émulateur et corrigés :
 * 1) une couche transparente `offset(y = -56.dp)` gardait 56 dp d'espace en trop sous chaque champ de date ;
 * 2) cette couche interceptait les taps du bouton « Effacer » (le sélecteur s'ouvrait au lieu d'effacer la date).
 */
class DatePickerFieldWiringTest {

    private val source = listOf(
        "src/main/kotlin/com/dmb/jobtracker/ui/joboffer/JobOfferFormSheet.kt",
        "androidApp/src/main/kotlin/com/dmb/jobtracker/ui/joboffer/JobOfferFormSheet.kt",
    ).map(::File).first { it.exists() }.readText()

    private val field = source.substringAfter("private fun DatePickerField(").substringBefore("if (showPicker) {")
        .lines().filterNot { it.trim().startsWith("//") }.joinToString("\n")

    @Test
    fun dateField_hasNoOffsetOverlayThatKeepsItsLayoutSpace() {
        assertFalse(field.contains(".offset("), "une couche décalée réserve son espace dans la Column (espace en trop) et masque le champ")
        assertFalse(field.contains(".clickable"), "pas de couche cliquable superposée au champ (elle intercepte « Effacer »)")
    }

    @Test
    fun dateField_opensThePickerFromTheFieldsOwnPressInteraction() {
        assertTrue(field.contains("interactionSource = interactionSource"))
        assertTrue(field.contains("PressInteraction.Release"))
        assertTrue(field.contains("showPicker = true"))
    }

    @Test
    fun dateField_keepsItsClearButtonInTheTrailingIcon() {
        assertTrue(field.contains("trailingIcon"))
        assertTrue(field.contains("TextButton(onClick = onClear)"))
        assertTrue(field.contains("\"Effacer\""))
    }
}
