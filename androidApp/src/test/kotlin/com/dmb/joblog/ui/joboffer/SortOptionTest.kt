package com.dmb.joblog.ui.joboffer

import com.dmb.joblog.R
import com.dmb.joblog.i18n.StringResources
import kotlin.test.Test
import kotlin.test.assertEquals

class SortOptionTest {

    private fun nameOf(id: Int): String = R.string::class.java.fields.first { it.getInt(null) == id }.name

    @Test
    fun entries_areInTheOrderShownInTheSortMenu() {
        assertEquals(
            listOf(SortOption.DATE_DESC, SortOption.DATE_ASC, SortOption.ALPHA_ASC, SortOption.ALPHA_DESC),
            SortOption.entries,
        )
    }

    @Test
    fun labelRes_everyOption_pointsToItsSortKey() {
        assertEquals(
            listOf("sort_newest", "sort_oldest", "sort_az", "sort_za"),
            SortOption.entries.map { nameOf(it.labelRes) },
        )
    }

    @Test
    fun labels_inEachLanguage_areTheDocumentedOnes() {
        val keys = SortOption.entries.map { nameOf(it.labelRes) }
        assertEquals(listOf("Newest first", "Oldest first", "A → Z", "Z → A"), keys.map { StringResources.androidEn.getValue(it) })
        assertEquals(listOf("Plus récent", "Plus ancien", "A → Z", "Z → A"), keys.map { StringResources.androidFr.getValue(it) })
    }
}
