package com.dmb.joblog

import java.io.File
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class DatePickerFieldWiringTest {

    private val source = listOf(
        "src/main/kotlin/com/dmb/joblog/ui/joboffer/JobOfferFormSheet.kt",
        "androidApp/src/main/kotlin/com/dmb/joblog/ui/joboffer/JobOfferFormSheet.kt",
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
        assertTrue(field.contains("R.string.form_clear"))
    }
}
