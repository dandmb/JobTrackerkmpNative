package com.dmb.joblog.ui.joboffer

import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals

/** Conversions propres à Android (DatePicker Material en millisecondes UTC). La logique de formulaire est testée dans sharedLogic. */
class DatePickerConversionsTest {

    @Test
    fun toDatePickerMillis_epochDate_isZero() {
        assertEquals(0L, LocalDate(1970, 1, 1).toDatePickerMillis())
    }

    @Test
    fun toDatePickerMillis_regularDate_isUtcMidnightInMilliseconds() {
        assertEquals(20_701L * 86_400_000L, LocalDate(2026, 9, 5).toDatePickerMillis())
    }

    @Test
    fun toDatePickerMillis_dateBefore1970_isNegative() {
        assertEquals(-86_400_000L, LocalDate(1969, 12, 31).toDatePickerMillis())
    }

    @Test
    fun datePickerMillisToLocalDate_utcMidnight_returnsThatDate() {
        assertEquals(LocalDate(2026, 9, 5), datePickerMillisToLocalDate(20_701L * 86_400_000L))
    }

    @Test
    fun datePickerMillisToLocalDate_lastMillisecondOfTheDay_staysOnTheSameDay() {
        assertEquals(LocalDate(2026, 9, 5), datePickerMillisToLocalDate(20_702L * 86_400_000L - 1))
    }

    @Test
    fun datePickerMillisToLocalDate_middleOfTheDay_staysOnTheSameDay() {
        assertEquals(LocalDate(2026, 9, 5), datePickerMillisToLocalDate(20_701L * 86_400_000L + 12 * 3_600_000L))
    }

    @Test
    fun datePickerRoundTrip_anyDate_isLossless() {
        listOf(LocalDate(2026, 9, 5), LocalDate(2028, 2, 29), LocalDate(1969, 12, 31), LocalDate(2026, 12, 31), LocalDate(2000, 1, 1))
            .forEach { assertEquals(it, datePickerMillisToLocalDate(it.toDatePickerMillis())) }
    }
}
