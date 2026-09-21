package com.dmb.joblog.ui.i18n

import com.dmb.joblog.i18n.AppLanguage
import java.util.Locale
import kotlin.test.Test
import kotlin.test.assertEquals

/** Détection de la langue depuis les `Locale` Android : français → FR ; anglais et TOUT le reste → EN. */
class AppLanguageAndroidTest {

    @Test
    fun french_inItsVariants_isFrench() {
        listOf(Locale.FRENCH, Locale.FRANCE, Locale.CANADA_FRENCH, Locale("fr", "CH"), Locale.forLanguageTag("fr-BE")).forEach {
            assertEquals(AppLanguage.FR, languageOf(it), "locale : $it")
        }
    }

    @Test
    fun english_andEveryOtherLanguage_isEnglish() {
        listOf(Locale.ENGLISH, Locale.US, Locale.UK, Locale.GERMAN, Locale("es", "ES"), Locale.JAPANESE, Locale("pt", "BR"), Locale.ITALIAN, Locale.ROOT).forEach {
            assertEquals(AppLanguage.EN, languageOf(it), "locale : $it")
        }
    }

    @Test
    fun defaultLanguage_isEnglish() {
        assertEquals(AppLanguage.EN, AppLanguage.DEFAULT)
    }
}
