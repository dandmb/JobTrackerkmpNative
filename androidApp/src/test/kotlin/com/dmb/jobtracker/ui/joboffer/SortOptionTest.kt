package com.dmb.jobtracker.ui.joboffer

import kotlin.test.Test
import kotlin.test.assertEquals

class SortOptionTest {

    @Test
    fun entries_areInTheOrderShownInTheSortMenu() {
        assertEquals(
            listOf(SortOption.DATE_DESC, SortOption.DATE_ASC, SortOption.ALPHA_ASC, SortOption.ALPHA_DESC),
            SortOption.entries,
        )
    }

    @Test
    fun label_everyOption_returnsItsMenuLabel() {
        assertEquals("Plus récent", SortOption.DATE_DESC.label)
        assertEquals("Plus ancien", SortOption.DATE_ASC.label)
        assertEquals("A → Z", SortOption.ALPHA_ASC.label)
        assertEquals("Z → A", SortOption.ALPHA_DESC.label)
    }
}
