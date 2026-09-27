package com.dmb.joblog.ui.util

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import androidx.core.net.toUri

/**
 * Ouvre une URL http(s) dans le navigateur (ou l'app associée, ex. LinkedIn) via `ACTION_VIEW`. Un intent implicite vers
 * un lien web est automatiquement visible malgré la visibilité des paquets d'Android 11+ (exemption système, voir
 * `ContactIntent.kt` pour le même principe appliqué au lien de messagerie) : aucune permission ni `<queries>` requis.
 * Renvoie `false` si rien ne sait l'ouvrir (défensif : n'arrive normalement jamais pour un lien http(s)).
 */
fun openUrl(context: Context, url: String): Boolean =
    try {
        context.startActivity(Intent(Intent.ACTION_VIEW, url.toUri()).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        true
    } catch (_: ActivityNotFoundException) {
        false
    }
