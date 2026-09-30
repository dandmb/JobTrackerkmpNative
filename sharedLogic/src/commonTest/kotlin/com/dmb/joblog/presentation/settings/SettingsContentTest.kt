package com.dmb.joblog.presentation.settings

import com.dmb.joblog.i18n.AppLanguage
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals

class SettingsContentTest {

    private val fr = SettingsContent.of(AppLanguage.FR)
    private val en = SettingsContent.of(AppLanguage.EN)

    @Test
    fun labels_inEachLanguage_areTheDocumentedOnes() {
        assertEquals("Réglages", fr.screenTitle)
        assertEquals("Settings", en.screenTitle)
        assertEquals("Retour", fr.backLabel)
        assertEquals("Back", en.backLabel)
        assertEquals("À propos", fr.aboutRowLabel)
        assertEquals("About", en.aboutRowLabel)
        assertEquals("Politique de confidentialité", fr.privacyRowLabel)
        assertEquals("Privacy Policy", en.privacyRowLabel)
        assertEquals("Mes documents", fr.documentsRowLabel)
        assertEquals("My documents", en.documentsRowLabel)
    }

    @Test
    fun entryPointLabel_isTheScreenTitle() {
        assertEquals(fr.screenTitle, fr.entryPointLabel)
        assertEquals(en.screenTitle, en.entryPointLabel)
    }

    @Test
    fun theRowLabels_areDistinctFromEachOther() {
        assertEquals(3, setOf(fr.aboutRowLabel, fr.privacyRowLabel, fr.documentsRowLabel).size)
        assertEquals(3, setOf(en.aboutRowLabel, en.privacyRowLabel, en.documentsRowLabel).size)
    }
}
