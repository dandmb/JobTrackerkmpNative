package com.dmb.joblog

import java.io.File
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Garde-fous (lecture du source) de la mise en page de l'écran principal Android. Défauts constatés à l'audit (police à
 * 200 %) et corrigés : la carte de statistiques ÉPINGLÉE mangeait jusqu'à ~60 % de l'écran ; une requête de recherche
 * résiduelle masquait l'état « aucune candidature ».
 */
class ListLayoutWiringTest {

    private val source = listOf(
        "src/main/kotlin/com/dmb/joblog/ui/joboffer/JobOfferListScreen.kt",
        "androidApp/src/main/kotlin/com/dmb/joblog/ui/joboffer/JobOfferListScreen.kt",
    ).map(::File).first { it.exists() }.readText()

    private val body = source.lines().filterNot { it.trim().startsWith("//") }.joinToString("\n")

    @Test
    fun statsCard_scrollsWithTheList_insideTheLazyColumn() {
        val lazy = body.indexOf("LazyColumn(")
        val stats = body.indexOf("JobOfferStatsCard(offers = state.offers)")

        assertTrue(lazy in 0 until stats, "la carte de stats doit être un item de la LazyColumn (défile avec la liste)")
        assertTrue(body.contains("item(key = \"stats\")"))
    }

    @Test
    fun searchField_staysPinnedAboveTheList() {
        val search = body.indexOf("OutlinedTextField(")
        val lazy = body.indexOf("LazyColumn(")

        assertTrue(search in 0 until lazy, "le champ de recherche reste épinglé au-dessus de la liste")
        assertTrue(body.contains("if (state.offers.isNotEmpty()) {\n                OutlinedTextField("))
    }

    @Test
    fun scrollTargetIndex_isShiftedByTheNumberOfItemsBeforeTheOffers() {
        assertTrue(body.contains("private const val STATS_ITEM_COUNT = 1"))
        assertTrue(body.contains("animateScrollToItem(target.index + STATS_ITEM_COUNT)"),
            "l'index de défilement après « Annuler » doit tenir compte de l'item de stats")
    }

    @Test
    fun emptyState_isCheckedBeforeTheNoResultMessage() {
        val whenBlock = body.substringAfter("state.isLoading ->").substringBefore("items(visibleOffers")
        assertTrue(whenBlock.indexOf("state.offers.isEmpty() -> EmptyOffersMessage()") in 0 until whenBlock.indexOf("R.string.list_no_results"),
            "« aucune candidature » doit primer sur « aucun résultat » (une requête résiduelle ne doit pas le masquer)")
        assertFalse(body.contains("visibleOffers.isEmpty() && searchQuery.isNotBlank() ->"), "ancienne condition (masquait l'état vide)")
    }

    @Test
    fun searchQuery_isClearedWhenTheListBecomesEmpty() {
        assertTrue(body.contains("LaunchedEffect(state.offers.isEmpty()) { if (state.offers.isEmpty()) searchQuery = \"\" }"))
    }

    @Test
    fun emptyState_multiLineTextsAreCentered() {
        val empty = body.substringAfter("private fun EmptyOffersMessage()")
        assertTrue(Regex("""Text\(stringResource\(R\.string\.list_empty_title\),[^)]*textAlign = TextAlign.Center""").containsMatchIn(empty))
        assertTrue(empty.substringAfter("R.string.list_empty_subtitle").contains("textAlign = TextAlign.Center"),
            "le sous-titre doit être centré même quand il passe sur plusieurs lignes")
    }
}
