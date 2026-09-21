package com.dmb.jobtracker

import java.io.File
import kotlin.test.Test
import kotlin.test.assertTrue

/** Garde-fou (lecture du source) : l'état vide de la liste garde son icône décorative « boîte vide » (Inbox), au-dessus du titre, et plus l'ancienne mallette. */
class EmptyStateIconWiringTest {

    private val source = listOf(
        "src/main/kotlin/com/dmb/jobtracker/ui/joboffer/JobOfferListScreen.kt",
        "androidApp/src/main/kotlin/com/dmb/jobtracker/ui/joboffer/JobOfferListScreen.kt",
    ).map(::File).first { it.exists() }.readText()

    private val emptyMessage = source.substringAfter("private fun EmptyOffersMessage()")

    @Test
    fun emptyOffersMessage_showsAnOutlinedInboxIconAboveTheTitle() {
        val icon = emptyMessage.indexOf("Icons.Outlined.Inbox")
        val title = emptyMessage.indexOf("\"Aucune candidature\"")

        assertTrue(icon in 0 until title, "l'icône doit précéder le titre « Aucune candidature »")
    }

    @Test
    fun emptyOffersMessage_iconIsDecorativeAndMuted() {
        val iconBlock = emptyMessage.substringAfter("Icon(").substringBefore("Spacer")

        assertTrue(iconBlock.contains("contentDescription = null"), "icône décorative : pas de description (le titre suffit à TalkBack)")
        assertTrue(iconBlock.contains("colorScheme.onSurfaceVariant"), "couleur atténuée attendue")
        assertTrue(iconBlock.contains("size(48.dp)"))
    }

    @Test
    fun emptyOffersMessage_noLongerUsesTheLiteralWorkBriefcase() {
        assertTrue(!emptyMessage.contains("WorkOutline"), "l'icône de l'état vide n'est plus la mallette (choix UX : boîte vide)")
    }
}
