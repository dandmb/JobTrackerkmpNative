package com.dmb.joblog.i18n

import com.dmb.joblog.testutil.jobOffer
import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals

class AppLanguageTest {

    @Test
    fun default_isEnglish() {
        assertEquals(AppLanguage.EN, AppLanguage.DEFAULT)
    }

    @Test
    fun fromTag_french_inEveryUsualSpelling() {
        listOf("fr", "FR", "fr-FR", "fr_FR", "fr-CA", "fr-CH", "  fr  ", "Fr-be").forEach {
            assertEquals(AppLanguage.FR, AppLanguage.fromTag(it), "balise : « $it »")
        }
    }

    @Test
    fun fromTag_englishAndEveryOtherLanguage_isEnglish() {
        listOf("en", "en-US", "en_GB", "es", "de-DE", "pt-BR", "ja", "ar", "zh-Hans", "und", "xx").forEach {
            assertEquals(AppLanguage.EN, AppLanguage.fromTag(it), "balise : « $it »")
        }
    }

    @Test
    fun fromTag_nullEmptyOrBlank_isEnglish() {
        listOf(null, "", "   ", "-", "_").forEach { assertEquals(AppLanguage.EN, AppLanguage.fromTag(it), "balise : « $it »") }
    }

    @Test
    fun fromTag_aLanguageThatOnlyStartsWithFr_isNotFrench() {
        listOf("fra", "frr", "fro", "fry").forEach { assertEquals(AppLanguage.EN, AppLanguage.fromTag(it), "balise : « $it »") }
    }

    @Test
    fun pick_returnsTheTextOfTheLanguage() {
        assertEquals("a", AppLanguage.EN.pick(en = "a", fr = "b"))
        assertEquals("b", AppLanguage.FR.pick(en = "a", fr = "b"))
    }

    // ---------- date courte de la carte ----------

    @Test
    fun shortDate_french_isDayThenAbbreviatedMonth() {
        assertEquals("5 sept.", ShortDate.format(LocalDate(2026, 9, 5), AppLanguage.FR))
        assertEquals("12 févr.", ShortDate.format(LocalDate(2026, 2, 12), AppLanguage.FR))
        assertEquals("31 déc.", ShortDate.format(LocalDate(2026, 12, 31), AppLanguage.FR))
    }

    @Test
    fun shortDate_english_isAbbreviatedMonthThenDay() {
        assertEquals("Sep 5", ShortDate.format(LocalDate(2026, 9, 5), AppLanguage.EN))
        assertEquals("Feb 12", ShortDate.format(LocalDate(2026, 2, 12), AppLanguage.EN))
        assertEquals("Dec 31", ShortDate.format(LocalDate(2026, 12, 31), AppLanguage.EN))
    }

    @Test
    fun shortDate_allTwelveMonths_inBothLanguages() {
        val fr = (1..12).map { ShortDate.format(LocalDate(2026, it, 1), AppLanguage.FR) }
        val en = (1..12).map { ShortDate.format(LocalDate(2026, it, 1), AppLanguage.EN) }

        assertEquals(listOf("1 janv.", "1 févr.", "1 mars", "1 avr.", "1 mai", "1 juin", "1 juil.", "1 août", "1 sept.", "1 oct.", "1 nov.", "1 déc."), fr)
        assertEquals(listOf("Jan 1", "Feb 1", "Mar 1", "Apr 1", "May 1", "Jun 1", "Jul 1", "Aug 1", "Sep 1", "Oct 1", "Nov 1", "Dec 1"), en)
    }

    @Test
    fun shortDate_dayHasNoLeadingZero() {
        assertEquals("1 mars", ShortDate.format(LocalDate(2026, 3, 1), AppLanguage.FR))
        assertEquals("Mar 1", ShortDate.format(LocalDate(2026, 3, 1), AppLanguage.EN))
    }
}
