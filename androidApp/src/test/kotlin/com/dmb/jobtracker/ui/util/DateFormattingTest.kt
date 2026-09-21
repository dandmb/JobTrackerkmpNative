package com.dmb.jobtracker.ui.util

import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals

class DateFormattingTest {

    @Test
    fun toShortFrenchDate_everyMonth_usesTheFrenchAbbreviation() {
        val expected = listOf(
            "5 janv.", "5 févr.", "5 mars", "5 avr.", "5 mai", "5 juin",
            "5 juil.", "5 août", "5 sept.", "5 oct.", "5 nov.", "5 déc.",
        )

        expected.forEachIndexed { index, text ->
            assertEquals(text, LocalDate(2026, index + 1, 5).toShortFrenchDate(), "mois ${index + 1}")
        }
    }

    @Test
    fun toShortFrenchDate_singleDigitDay_hasNoLeadingZero() {
        assertEquals("1 mars", LocalDate(2026, 3, 1).toShortFrenchDate())
        assertEquals("9 juil.", LocalDate(2026, 7, 9).toShortFrenchDate())
    }

    @Test
    fun toShortFrenchDate_twoDigitDay_isPrintedInFull() {
        assertEquals("10 oct.", LocalDate(2026, 10, 10).toShortFrenchDate())
        assertEquals("31 déc.", LocalDate(2026, 12, 31).toShortFrenchDate())
    }

    @Test
    fun toShortFrenchDate_leapDay_isFebruary29() {
        assertEquals("29 févr.", LocalDate(2028, 2, 29).toShortFrenchDate())
    }

    @Test
    fun toShortFrenchDate_year_isNotPrinted() {
        assertEquals(LocalDate(1999, 9, 5).toShortFrenchDate(), LocalDate(2026, 9, 5).toShortFrenchDate())
    }

    @Test
    fun toShortFrenchDate_monthsWithoutAbbreviationPoint_areFullWords() {
        // mars, mai, juin, août ne sont pas abrégés (comme le formatage fr_FR d'iOS).
        listOf(3, 5, 6, 8).forEach { month ->
            val text = LocalDate(2026, month, 5).toShortFrenchDate()
            assertEquals(false, text.endsWith("."), text)
        }
    }
}
