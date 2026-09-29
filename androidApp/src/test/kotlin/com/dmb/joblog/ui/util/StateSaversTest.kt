package com.dmb.joblog.ui.util

import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.SaverScope
import com.dmb.joblog.domain.model.ApplicationStatus
import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class StateSaversTest {

    private val scope = SaverScope { true }

    private fun <T, S : Any> roundTrip(saver: Saver<T, S>, value: T): T? {
        val saved = with(saver) { scope.save(value) } ?: return null
        return saver.restore(saved)
    }

    @Test
    fun localDate_survivesASaveRestoreCycle() {
        assertEquals(LocalDate(2026, 2, 28), roundTrip(LocalDateSaver, LocalDate(2026, 2, 28)))
    }

    @Test
    fun optionalLocalDate_keepsBothADateAndNull() {
        assertEquals(LocalDate(2026, 9, 30), roundTrip(OptionalLocalDateSaver, LocalDate(2026, 9, 30)))
        assertNull(with(OptionalLocalDateSaver) { scope.save(null) }, "une date effacée reste effacée")
    }

    @Test
    fun statusSet_survivesASaveRestoreCycle() {
        val selection = setOf(ApplicationStatus.INTERVIEW, ApplicationStatus.REJECTED)
        assertEquals(selection, roundTrip(StatusSetSaver, selection))
        assertEquals(emptySet(), roundTrip(StatusSetSaver, emptySet()))
    }
}
