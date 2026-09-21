package com.dmb.jobtracker.ui.theme

import androidx.compose.ui.graphics.Color

// Primaire — Teal profond, calme et professionnel
val Teal40 = Color(0xFF0D6E68)
val Teal80 = Color(0xFF6FD4C8)
val Teal20 = Color(0xFF00201B)
val TealContainerLight = Color(0xFFB0F1E4)
val TealContainerDark = Color(0xFF00504A)

// Secondaire — Corail chaud, pour les accents et l'énergie (stats, CTA)
val Coral40 = Color(0xFFE8734A)
val Coral80 = Color(0xFFFFB59D)
val CoralContainerLight = Color(0xFFFFDBCB)
val CoralContainerDark = Color(0xFF7D2C0C)

// Neutres
val SurfaceLight = Color(0xFFF7FBF9)
val OnSurfaceLight = Color(0xFF161D1C)
val SurfaceDark = Color(0xFF0F1514)
val OnSurfaceDark = Color(0xFFDDE4E1)

// Erreur (thème clair) — était StatusRejected
val ErrorLight = Color(0xFFBA1A1A)

// Couleurs sémantiques par statut — utilisées comme COULEUR DE TEXTE (chip de la carte, badges de la carte stats).
// Chaque variante atteint WCAG AA (>= 4.5:1) sur les deux fonds où elle s'affiche :
//   - la Card (surfaceContainerHighest : #E6E0E9 clair / #36343B sombre)
//   - un badge de la carte stats (couleur teintée à 16 % clair / 8 % sombre sur primaryContainer)
// Les teintes de marque (teal, corail) sont conservées ; seule la luminosité change.
val StatusPendingLight = Color(0xFF57535A)   // 5.8:1 carte · 4.8:1 badge
val StatusAppliedLight = Color(0xFF0B5E58)   // 5.9 · 4.8
val StatusInterviewLight = Color(0xFF913312) // 6.0 · 4.8   (Coral40 #E8734A ne donnait que 2.3:1)
val StatusRejectedLight = Color(0xFF9F1616)  // 6.2 · 4.8
val StatusAcceptedLight = Color(0xFF235F26)  // 5.9 · 4.8

val StatusPendingDark = Color(0xFFCAC4D0)    // 7.2 · 4.8
val StatusAppliedDark = Color(0xFF78DDD1)    // 7.6 · 5.0
val StatusInterviewDark = Color(0xFFFFB59D)  // 7.2 · 4.9   (= Coral80)
val StatusRejectedDark = Color(0xFFFFB4AB)   // 7.2 · 4.9   (= error sombre M3)
val StatusAcceptedDark = Color(0xFF9BD99F)   // 7.5 · 4.9

// Opacité de la teinte de fond des badges de statut. Plus faible en sombre : une teinte plus forte
// éclaircit le fond du badge et fait passer le texte sous 4.5:1.
const val StatusBadgeTintLight = 0.16f
const val StatusBadgeTintDark = 0.08f
