package com.dmb.joblog.presentation.onboarding

import com.dmb.joblog.i18n.AppLanguage
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

/** Contenu et règles de navigation de l'onboarding, communs à Android et iOS, dans les deux langues. */
class OnboardingContentTest {

    private val fr = OnboardingContent.of(AppLanguage.FR)
    private val en = OnboardingContent.of(AppLanguage.EN)
    private val both = listOf(fr, en)

    // ---------- contenu ----------

    @Test
    fun pages_areExactlyThree_inEachLanguage() {
        both.forEach {
            assertEquals(3, it.pages.size)
            assertEquals(3, it.pageCount)
        }
    }

    @Test
    fun pages_everyTitleAndDescription_isNotBlank() {
        both.forEach { c -> c.pages.forEach { assertTrue(it.title.isNotBlank() && it.description.isNotBlank()) } }
    }

    @Test
    fun pages_titles_areAllDistinct() {
        both.forEach { assertEquals(3, it.pages.map { p -> p.title }.toSet().size) }
    }

    @Test
    fun pages_titlesAreShort_soTheyFitOnOneOrTwoLines() {
        both.forEach { c -> c.pages.forEach { assertTrue(it.title.length <= 40, "titre trop long : ${it.title}") } }
    }

    @Test
    fun pages_frenchWording_isTheDocumentedOne() {
        assertEquals(listOf("Suis tes candidatures", "Garde un œil sur les statuts", "Vois où tu en es"), fr.pages.map { it.title })
    }

    @Test
    fun pages_englishWording_isTheDocumentedOne() {
        assertEquals(listOf("Track your applications", "Keep an eye on statuses", "See where you stand"), en.pages.map { it.title })
    }

    @Test
    fun everyPage_isTranslated_soNothingStaysInTheOtherLanguage() {
        fr.pages.zip(en.pages).forEach { (f, e) ->
            assertNotEquals(f.title, e.title)
            assertNotEquals(f.description, e.description)
        }
        listOf(fr.skipLabel to en.skipLabel, fr.nextLabel to en.nextLabel, fr.startLabel to en.startLabel).forEach { (f, e) -> assertNotEquals(f, e) }
    }

    @Test
    fun englishTexts_containNoFrenchAccent() {
        val accents = Regex("[àâçéèêëîïôûùüÿœ]", RegexOption.IGNORE_CASE)
        val text = en.pages.joinToString(" ") { it.title + " " + it.description } + en.skipLabel + en.nextLabel + en.startLabel
        assertFalse(accents.containsMatchIn(text), "texte français oublié dans la version anglaise")
    }

    @Test
    fun statusesPage_usesTheStatusVocabularyOfTheApp_inEachLanguage() {
        listOf("En attente", "Postulé", "Entretien", "Accepté", "Refusé").forEach {
            assertTrue(fr.pages[1].description.contains(it), "« $it » absent de la page statuts (FR)")
        }
        listOf("Pending", "Applied", "Interview", "Accepted", "Rejected").forEach {
            assertTrue(en.pages[1].description.contains(it), "« $it » absent de la page statuts (EN)")
        }
    }

    // ---------- navigation (identique dans les deux langues) ----------

    @Test
    fun isLastPage_onlyTheThirdPage() {
        both.forEach {
            assertFalse(it.isLastPage(0))
            assertFalse(it.isLastPage(1))
            assertTrue(it.isLastPage(2))
            assertTrue(it.isLastPage(5))
        }
    }

    @Test
    fun showsSkip_onNonFinalPagesOnly() {
        both.forEach {
            assertTrue(it.showsSkip(0))
            assertTrue(it.showsSkip(1))
            assertFalse(it.showsSkip(2))
        }
    }

    @Test
    fun primaryButtonLabel_isNextThenStartOnTheLastPage() {
        assertEquals(listOf("Suivant", "Suivant", "Commencer"), (0..2).map { fr.primaryButtonLabel(it) })
        assertEquals(listOf("Next", "Next", "Get started"), (0..2).map { en.primaryButtonLabel(it) })
    }

    @Test
    fun buttonLabels_matchTheDocumentedWording() {
        assertEquals(Triple("Passer", "Suivant", "Commencer"), Triple(fr.skipLabel, fr.nextLabel, fr.startLabel))
        assertEquals(Triple("Skip", "Next", "Get started"), Triple(en.skipLabel, en.nextLabel, en.startLabel))
    }

    @Test
    fun nextPageIndex_movesForwardAndIsClamped() {
        both.forEach {
            assertEquals(1, it.nextPageIndex(0))
            assertEquals(2, it.nextPageIndex(1))
            assertEquals(2, it.nextPageIndex(2))
            assertEquals(0, it.nextPageIndex(-5))
        }
    }

    @Test
    fun walkingThroughEveryPage_endsOnTheStartButtonWithoutSkip() {
        both.forEach { c ->
            var index = 0
            var steps = 0
            while (!c.isLastPage(index)) {
                assertTrue(c.showsSkip(index))
                index = c.nextPageIndex(index)
                steps++
            }
            assertEquals(2, steps)
            assertEquals(c.startLabel, c.primaryButtonLabel(index))
            assertFalse(c.showsSkip(index))
        }
    }
}
