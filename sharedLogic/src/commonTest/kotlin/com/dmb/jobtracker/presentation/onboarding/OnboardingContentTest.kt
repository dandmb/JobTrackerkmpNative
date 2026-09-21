package com.dmb.jobtracker.presentation.onboarding

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/** Contenu et règles de navigation de l'onboarding, communs à Android et iOS. */
class OnboardingContentTest {

    private val content = OnboardingContent

    // ---------- contenu ----------

    @Test
    fun pages_areExactlyThree() {
        assertEquals(3, content.pages.size)
        assertEquals(3, content.pageCount)
    }

    @Test
    fun pages_everyTitleAndDescription_isNotBlank() {
        content.pages.forEach {
            assertTrue(it.title.isNotBlank())
            assertTrue(it.description.isNotBlank())
        }
    }

    @Test
    fun pages_titles_areAllDistinct() {
        assertEquals(3, content.pages.map { it.title }.toSet().size)
    }

    @Test
    fun pages_titlesAreShort_soTheyFitOnOneOrTwoLines() {
        content.pages.forEach { assertTrue(it.title.length <= 40, "titre trop long : ${it.title}") }
    }

    @Test
    fun pages_coverTheThreeTopics_applicationsThenStatusesThenStats() {
        assertEquals("Suis tes candidatures", content.pages[0].title)
        assertEquals("Garde un œil sur les statuts", content.pages[1].title)
        assertEquals("Vois où tu en es", content.pages[2].title)
    }

    @Test
    fun statusesPage_usesTheStatusVocabularyOfTheApp() {
        val description = content.pages[1].description

        listOf("En attente", "Postulé", "Entretien", "Accepté", "Refusé").forEach {
            assertTrue(description.contains(it), "« $it » absent de la page statuts")
        }
    }

    // ---------- navigation ----------

    @Test
    fun isLastPage_onlyTheThirdPage() {
        assertFalse(content.isLastPage(0))
        assertFalse(content.isLastPage(1))
        assertTrue(content.isLastPage(2))
    }

    @Test
    fun isLastPage_indexBeyondTheEnd_isStillLast() {
        assertTrue(content.isLastPage(5))
    }

    @Test
    fun showsSkip_onNonFinalPagesOnly() {
        assertTrue(content.showsSkip(0))
        assertTrue(content.showsSkip(1))
        assertFalse(content.showsSkip(2))
    }

    @Test
    fun primaryButtonLabel_isNextThenStartOnTheLastPage() {
        assertEquals("Suivant", content.primaryButtonLabel(0))
        assertEquals("Suivant", content.primaryButtonLabel(1))
        assertEquals("Commencer", content.primaryButtonLabel(2))
    }

    @Test
    fun buttonLabels_matchTheDocumentedFrenchWording() {
        assertEquals("Passer", OnboardingContent.SKIP_LABEL)
        assertEquals("Suivant", OnboardingContent.NEXT_LABEL)
        assertEquals("Commencer", OnboardingContent.START_LABEL)
    }

    @Test
    fun nextPageIndex_movesForwardOnePage() {
        assertEquals(1, content.nextPageIndex(0))
        assertEquals(2, content.nextPageIndex(1))
    }

    @Test
    fun nextPageIndex_onTheLastPage_staysOnTheLastPage() {
        assertEquals(2, content.nextPageIndex(2))
    }

    @Test
    fun nextPageIndex_negativeIndex_isClampedToTheFirstPage() {
        assertEquals(0, content.nextPageIndex(-5))
    }

    @Test
    fun walkingThroughEveryPage_endsOnTheStartButtonWithoutSkip() {
        var index = 0
        var steps = 0
        while (!content.isLastPage(index)) {
            assertTrue(content.showsSkip(index))
            index = content.nextPageIndex(index)
            steps++
        }

        assertEquals(2, steps)
        assertEquals("Commencer", content.primaryButtonLabel(index))
        assertFalse(content.showsSkip(index))
    }
}
