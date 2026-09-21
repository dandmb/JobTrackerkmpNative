package com.dmb.jobtracker

import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Garde-fou (lecture du source) : l'action de tri de la barre du haut n'est composée que s'il existe au moins une candidature
 * (`state.offers`, toutes les offres), et NON en fonction du résultat filtré par la recherche (`visibleOffers`).
 */
class SortActionVisibilityWiringTest {

    private val source = listOf(
        "src/main/kotlin/com/dmb/jobtracker/ui/joboffer/JobOfferListScreen.kt",
        "androidApp/src/main/kotlin/com/dmb/jobtracker/ui/joboffer/JobOfferListScreen.kt",
    ).map(::File).first { it.exists() }.readText()

    /** Le contenu de `actions = { … }` du `TopAppBar`, jusqu'aux `colors = `. */
    private val actions = source.substringAfter("actions = {").substringBefore("colors = TopAppBarDefaults")
        .lines().filterNot { it.trim().startsWith("//") }.joinToString("\n")   // les commentaires peuvent citer les noms interdits

    @Test
    fun sortAction_isOnlyComposedWhenAtLeastOneOfferExists() {
        val condition = actions.indexOf("if (state.offers.isNotEmpty())")
        val sortButton = actions.indexOf("Icons.Default.Sort")
        val dropdown = actions.indexOf("DropdownMenu(")

        assertTrue(condition >= 0, "condition `state.offers.isNotEmpty()` absente de la barre du haut")
        assertTrue(sortButton > condition, "le bouton de tri doit être DANS la condition")
        assertTrue(dropdown > condition, "le menu déroulant de tri doit être DANS la condition")
    }

    @Test
    fun sortAction_conditionUsesAllOffersNotTheSearchFilteredOnes() {
        assertFalse(actions.contains("visibleOffers"), "la visibilité du tri ne doit pas dépendre du filtre de recherche")
        assertFalse(actions.contains("searchQuery"), "la visibilité du tri ne doit pas dépendre de la recherche")
    }

    @Test
    fun aboutAction_staysAlwaysVisible() {
        val condition = actions.indexOf("if (state.offers.isNotEmpty())")
        val about = actions.indexOf("onOpenAbout")

        assertTrue(about in 0 until condition, "l'icône ⓘ doit rester hors de la condition (visible même liste vide)")
    }

    @Test
    fun sortMenuState_isResetWhenTheListBecomesEmpty() {
        assertTrue(source.contains("if (state.offers.isEmpty()) sortMenuExpanded = false"), "le menu ne doit pas se rouvrir seul après un retour de données")
        assertEquals(1, Regex("""if \(state\.offers\.isNotEmpty\(\)\)\s*\{\s*Box \{""").findAll(actions).count())
    }
}
