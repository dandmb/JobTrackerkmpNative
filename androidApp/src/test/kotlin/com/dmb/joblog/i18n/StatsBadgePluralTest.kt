package com.dmb.joblog.i18n

import kotlin.test.Test
import kotlin.test.assertEquals

class StatsBadgePluralTest {

    private val expectedFrenchPlurals = mapOf(
        "stats_applied_other" to "%1\$d postulés",
        "stats_interview_other" to "%1\$d entretiens",
        "stats_rejected_other" to "%1\$d refusés",
        "stats_accepted_other" to "%1\$d acceptés",
    )

    @Test
    fun frenchBadges_takeThePluralFromTwo_onAndroid() {
        expectedFrenchPlurals.forEach { (key, value) -> assertEquals(value, StringResources.androidFr[key], key) }
    }

    @Test
    fun frenchBadges_takeThePluralFromTwo_onIos() {
        expectedFrenchPlurals.forEach { (key, value) -> assertEquals(value, StringResources.iosFr[key], key) }
    }
}
