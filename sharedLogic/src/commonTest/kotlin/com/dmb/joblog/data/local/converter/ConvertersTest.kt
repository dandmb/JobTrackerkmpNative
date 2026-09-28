package com.dmb.joblog.data.local.converter

import com.dmb.joblog.domain.model.ApplicationStatus
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class ConvertersTest {

    private val converters = Converters()

    @Test
    fun fromStatus_everyStatus_returnsItsEnumName() {
        val expected = mapOf(
            ApplicationStatus.PENDING to "PENDING",
            ApplicationStatus.APPLIED to "APPLIED",
            ApplicationStatus.INTERVIEW to "INTERVIEW",
            ApplicationStatus.REJECTED to "REJECTED",
            ApplicationStatus.ACCEPTED to "ACCEPTED",
        )

        expected.forEach { (status, name) -> assertEquals(name, converters.fromStatus(status)) }
        assertEquals(ApplicationStatus.entries.size, expected.size, "un statut n'est pas couvert par ce test")
    }

    @Test
    fun toStatus_everyKnownName_returnsTheMatchingStatus() {
        ApplicationStatus.entries.forEach { status ->
            assertEquals(status, converters.toStatus(status.name))
        }
    }

    @Test
    fun toStatus_unknownValue_throwsIllegalArgument() {
        assertFailsWith<IllegalArgumentException> { converters.toStatus("ARCHIVED") }
    }

    @Test
    fun toStatus_wrongCase_throwsIllegalArgument() {
        assertFailsWith<IllegalArgumentException> { converters.toStatus("applied") }
    }

    @Test
    fun toStatus_emptyString_throwsIllegalArgument() {
        assertFailsWith<IllegalArgumentException> { converters.toStatus("") }
    }
}
