package com.dmb.joblog.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.luminance
import kotlin.math.max
import kotlin.math.min
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Garde-fou d'accessibilité : les couleurs de statut servent de TEXTE (chip de la carte, badges de la carte stats).
 * Chacune doit atteindre WCAG AA (>= 4.5:1) sur les fonds où elle s'affiche, dans les deux modes.
 * Si quelqu'un modifie une couleur ou une opacité de teinte, ce test dit immédiatement si le contraste régresse.
 */
class StatusColorsTest {

    // Fonds Material 3 réellement utilisés (voir PROJECT_CONTEXT.md §4).
    private val cardLight = Color(0xFFE6E0E9)      // surfaceContainerHighest, clair
    private val cardDark = Color(0xFF36343B)       // surfaceContainerHighest, sombre
    private val statsCardLight = TealContainerLight // primaryContainer, clair
    private val statsCardDark = TealContainerDark   // primaryContainer, sombre

    private fun contrast(a: Color, b: Color): Double {
        val la = a.luminance().toDouble()
        val lb = b.luminance().toDouble()
        return (max(la, lb) + 0.05) / (min(la, lb) + 0.05)
    }

    private fun StatusPalette.all() = mapOf(
        "PENDING" to pending, "APPLIED" to applied, "INTERVIEW" to interview,
        "REJECTED" to rejected, "ACCEPTED" to accepted,
    )

    private fun assertAa(palette: StatusPalette, card: Color, statsCard: Color, mode: String) {
        palette.all().forEach { (name, color) ->
            val onCard = contrast(color, card)
            assertTrue(onCard >= 4.5, "$mode $name sur la carte : $onCard < 4.5")

            val badgeBackground = color.copy(alpha = palette.badgeTintAlpha).compositeOver(statsCard)
            val onBadge = contrast(color, badgeBackground)
            assertTrue(onBadge >= 4.5, "$mode $name sur un badge de la carte stats : $onBadge < 4.5")
        }
    }

    @Test
    fun lightPalette_everyStatusColor_reachesAaContrastOnCardAndBadge() =
        assertAa(LightStatusPalette, cardLight, statsCardLight, "clair")

    @Test
    fun darkPalette_everyStatusColor_reachesAaContrastOnCardAndBadge() =
        assertAa(DarkStatusPalette, cardDark, statsCardDark, "sombre")

    @Test
    fun lightPalette_statusColors_areAllDistinct() {
        assertEquals(5, LightStatusPalette.all().values.toSet().size)
    }

    @Test
    fun darkPalette_statusColors_areAllDistinct() {
        assertEquals(5, DarkStatusPalette.all().values.toSet().size)
    }

    @Test
    fun badgeTintAlpha_isLighterInDarkModeThanInLightMode() {
        // Une teinte plus forte éclaircit le fond du badge et fait passer le texte sous 4.5:1 en sombre.
        assertEquals(0.16f, LightStatusPalette.badgeTintAlpha)
        assertEquals(0.08f, DarkStatusPalette.badgeTintAlpha)
        assertTrue(DarkStatusPalette.badgeTintAlpha < LightStatusPalette.badgeTintAlpha)
    }

    @Test
    fun palettes_matchTheDocumentedHexValues() {
        assertEquals(Color(0xFF57535A), LightStatusPalette.pending)
        assertEquals(Color(0xFF0B5E58), LightStatusPalette.applied)
        assertEquals(Color(0xFF913312), LightStatusPalette.interview)
        assertEquals(Color(0xFF9F1616), LightStatusPalette.rejected)
        assertEquals(Color(0xFF235F26), LightStatusPalette.accepted)
        assertEquals(Color(0xFFCAC4D0), DarkStatusPalette.pending)
        assertEquals(Color(0xFF78DDD1), DarkStatusPalette.applied)
        assertEquals(Color(0xFFFFB59D), DarkStatusPalette.interview)
        assertEquals(Color(0xFFFFB4AB), DarkStatusPalette.rejected)
        assertEquals(Color(0xFF9BD99F), DarkStatusPalette.accepted)
    }

    @Test
    fun contrastHelper_blackOnWhite_is21ToOne() {
        // Vérifie l'outil de mesure lui-même (valeur de référence WCAG).
        assertEquals(21.0, contrast(Color.Black, Color.White), 0.01)
    }

    @Test
    fun contrastHelper_sameColor_isOneToOne() {
        assertEquals(1.0, contrast(Color.Red, Color.Red), 0.0001)
    }
}
