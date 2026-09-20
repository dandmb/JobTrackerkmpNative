package com.dmb.jobtracker.ui.joboffer

import com.dmb.jobtracker.domain.model.ApplicationStatus
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class StatusLabelsTest {

    @Test
    fun displayLabel_everyStatus_returnsItsFrenchLabel() {
        val expected = mapOf(
            ApplicationStatus.PENDING to "En attente",
            ApplicationStatus.APPLIED to "Postulé",
            ApplicationStatus.INTERVIEW to "Entretien",
            ApplicationStatus.REJECTED to "Refusé",
            ApplicationStatus.ACCEPTED to "Accepté",
        )

        expected.forEach { (status, label) -> assertEquals(label, status.displayLabel()) }
        assertEquals(ApplicationStatus.entries.size, expected.size, "un statut n'est pas couvert par ce test")
    }

    @Test
    fun shortLabel_everyStatus_returnsItsLowercaseShortLabel() {
        val expected = mapOf(
            ApplicationStatus.PENDING to "attente",
            ApplicationStatus.APPLIED to "postulé",
            ApplicationStatus.INTERVIEW to "entretien",
            ApplicationStatus.REJECTED to "refusé",
            ApplicationStatus.ACCEPTED to "accepté",
        )

        expected.forEach { (status, label) -> assertEquals(label, status.shortLabel()) }
        assertEquals(ApplicationStatus.entries.size, expected.size, "un statut n'est pas couvert par ce test")
    }

    @Test
    fun displayLabels_areAllDistinct() {
        assertEquals(ApplicationStatus.entries.size, ApplicationStatus.entries.map { it.displayLabel() }.toSet().size)
    }

    @Test
    fun shortLabels_areTheLowercaseFormOfTheFullLabelExceptForPending() {
        ApplicationStatus.entries.filter { it != ApplicationStatus.PENDING }.forEach {
            assertEquals(it.displayLabel().lowercase(), it.shortLabel())
        }
        assertTrue(ApplicationStatus.PENDING.shortLabel() == "attente")
    }
}
