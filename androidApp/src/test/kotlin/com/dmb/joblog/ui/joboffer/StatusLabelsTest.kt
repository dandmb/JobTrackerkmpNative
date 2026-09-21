package com.dmb.joblog.ui.joboffer

import com.dmb.joblog.R
import com.dmb.joblog.domain.model.ApplicationStatus
import com.dmb.joblog.i18n.StringResources
import kotlin.test.Test
import kotlin.test.assertEquals

/** Les libellés de statut sont des ressources (anglais / français) ; ici : chaque statut pointe la bonne clé, et les textes sont ceux attendus. */
class StatusLabelsTest {

    private fun nameOf(id: Int): String = R.string::class.java.fields.first { it.getInt(null) == id }.name

    @Test
    fun labelRes_everyStatus_pointsToItsStatusKey() {
        val expected = mapOf(
            ApplicationStatus.PENDING to "status_pending", ApplicationStatus.APPLIED to "status_applied",
            ApplicationStatus.INTERVIEW to "status_interview", ApplicationStatus.REJECTED to "status_rejected",
            ApplicationStatus.ACCEPTED to "status_accepted",
        )
        expected.forEach { (status, key) -> assertEquals(key, nameOf(status.labelRes())) }
        assertEquals(ApplicationStatus.entries.size, expected.size, "un statut n'est pas couvert par ce test")
    }

    @Test
    fun labels_inEnglishAndFrench_areTheDocumentedOnes() {
        val fr = mapOf("status_pending" to "En attente", "status_applied" to "Postulé", "status_interview" to "Entretien", "status_rejected" to "Refusé", "status_accepted" to "Accepté")
        val en = mapOf("status_pending" to "Pending", "status_applied" to "Applied", "status_interview" to "Interview", "status_rejected" to "Rejected", "status_accepted" to "Accepted")
        assertEquals(fr, fr.keys.associateWith { StringResources.androidFr.getValue(it) })
        assertEquals(en, en.keys.associateWith { StringResources.androidEn.getValue(it) })
    }

    @Test
    fun labels_areAllDistinct_inEachLanguage() {
        listOf(StringResources.androidEn, StringResources.androidFr).forEach { strings ->
            val labels = ApplicationStatus.entries.map { strings.getValue(nameOf(it.labelRes())) }
            assertEquals(ApplicationStatus.entries.size, labels.toSet().size)
        }
    }

    @Test
    fun shortLabelRes_singularForOne_pluralForMore() {
        ApplicationStatus.entries.forEach {
            val key = nameOf(it.labelRes()).removePrefix("status_")
            assertEquals("stats_${key}_one", nameOf(it.shortLabelRes(1)))
            assertEquals("stats_${key}_other", nameOf(it.shortLabelRes(2)))
            assertEquals("stats_${key}_other", nameOf(it.shortLabelRes(10)))
        }
    }

    @Test
    fun shortLabels_carryTheCount_andOnlyInterviewChangesWithThePluralInEnglish() {
        val en = StringResources.androidEn
        assertEquals("%1\$d interview", en.getValue("stats_interview_one"))
        assertEquals("%1\$d interviews", en.getValue("stats_interview_other"))
        assertEquals(en.getValue("stats_applied_one"), en.getValue("stats_applied_other"))
        assertEquals("%1\$d postulé", StringResources.androidFr.getValue("stats_applied_one"))
    }
}
