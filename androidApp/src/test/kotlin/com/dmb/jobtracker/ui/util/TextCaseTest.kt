package com.dmb.jobtracker.ui.util

import kotlin.test.Test
import kotlin.test.assertEquals

class TextCaseTest {

    // ---------- toTitleCase ----------

    @Test
    fun toTitleCase_lowercaseWords_capitalizesEachWord() {
        assertEquals("Développeur Android Senior", "développeur android senior".toTitleCase())
    }

    @Test
    fun toTitleCase_mixedCaseBrandStartingWithLowercase_isKeptAsIs() {
        assertEquals("iOS Engineer", "iOS engineer".toTitleCase())
        assertEquals("iPhone Designer", "iPhone designer".toTitleCase())
        assertEquals("eBay", "eBay".toTitleCase())
    }

    @Test
    fun toTitleCase_acronymAlreadyUppercase_isKeptAsIs() {
        assertEquals("Senior SQL Analyst", "senior SQL analyst".toTitleCase())
        assertEquals("QA", "QA".toTitleCase())
        assertEquals("UX/UI Designer", "UX/UI designer".toTitleCase())
    }

    @Test
    fun toTitleCase_wordAlreadyCapitalized_isUnchanged() {
        assertEquals("Lead Developer", "Lead Developer".toTitleCase())
    }

    @Test
    fun toTitleCase_wordWithInnerUppercaseLikeMcDonalds_isKeptAsIs() {
        assertEquals("McDonald's Manager", "McDonald's manager".toTitleCase())
    }

    @Test
    fun toTitleCase_emptyString_returnsEmptyString() {
        assertEquals("", "".toTitleCase())
    }

    @Test
    fun toTitleCase_singleLowercaseLetter_isCapitalized() {
        assertEquals("A", "a".toTitleCase())
    }

    @Test
    fun toTitleCase_multipleSpaces_arePreserved() {
        assertEquals("iPhone  Designer", "iPhone  designer".toTitleCase())
        assertEquals("A  B", "a  b".toTitleCase())
    }

    @Test
    fun toTitleCase_leadingAndTrailingSpaces_arePreserved() {
        assertEquals(" Dev", " dev".toTitleCase())
        assertEquals("Dev ", "dev ".toTitleCase())
        assertEquals("  ", "  ".toTitleCase())
    }

    @Test
    fun toTitleCase_hyphenatedWord_capitalizesOnlyTheFirstLetter() {
        assertEquals("Full-stack Dev", "full-stack dev".toTitleCase())
    }

    @Test
    fun toTitleCase_wordStartingWithDigit_isLeftAlone() {
        assertEquals("3D Artist", "3D artist".toTitleCase())
    }

    @Test
    fun toTitleCase_accentedFirstLetter_isCapitalizedWithItsAccent() {
        assertEquals("Élève Ingénieur", "élève ingénieur".toTitleCase())
    }

    @Test
    fun toTitleCase_wordWithDotInside_capitalizesFirstLetterOnly() {
        assertEquals("Node.js Developer", "node.js developer".toTitleCase())
    }

    @Test
    fun toTitleCase_entirelyLowercaseAcronym_isCapitalizedLikeAnyWord() {
        // Limite connue : sans dictionnaire de sigles, « ios » est indiscernable de n'importe quel mot.
        assertEquals("Ios Developer", "ios developer".toTitleCase())
    }

    // ---------- capitalizeFirst ----------

    @Test
    fun capitalizeFirst_lowercaseCompany_capitalizesOnlyTheFirstLetter() {
        assertEquals("Acme corp", "acme corp".capitalizeFirst())
    }

    @Test
    fun capitalizeFirst_alreadyCapitalized_isUnchanged() {
        assertEquals("Spotify", "Spotify".capitalizeFirst())
    }

    @Test
    fun capitalizeFirst_firstWordIsMixedCaseBrand_isKeptAsIs() {
        assertEquals("eBay", "eBay".capitalizeFirst())
        assertEquals("iRobot corp", "iRobot corp".capitalizeFirst())
    }

    @Test
    fun capitalizeFirst_fullyUppercaseCompany_isUnchanged() {
        assertEquals("ACME", "ACME".capitalizeFirst())
    }

    @Test
    fun capitalizeFirst_mixedCaseWordNotFirst_doesNotPreventCapitalizingTheFirstWord() {
        assertEquals("Acme eBay partners", "acme eBay partners".capitalizeFirst())
    }

    @Test
    fun capitalizeFirst_emptyString_returnsEmptyString() {
        assertEquals("", "".capitalizeFirst())
    }

    @Test
    fun capitalizeFirst_accentedFirstLetter_isCapitalized() {
        assertEquals("Élan", "élan".capitalizeFirst())
    }

    @Test
    fun capitalizeFirst_startsWithDigit_isUnchanged() {
        assertEquals("3M", "3M".capitalizeFirst())
    }

    @Test
    fun capitalizeFirst_leadingSpace_isLeftAlone() {
        assertEquals(" acme", " acme".capitalizeFirst())
    }
}
