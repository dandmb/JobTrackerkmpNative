package com.dmb.jobtracker.ui.about

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import androidx.core.net.toUri
import com.dmb.jobtracker.presentation.about.AboutContent

/** L'intention « écrire un e-mail » : `ACTION_SENDTO` + lien mailto (adresse et objet pré-rempli, définis dans sharedLogic). */
fun contactIntent(): Intent = Intent(Intent.ACTION_SENDTO, AboutContent.contactMailtoUri().toUri())

/**
 * Ouvre l'application de messagerie (ou le sélecteur d'applications s'il y en a plusieurs).
 * Renvoie `false` si aucune application ne sait gérer lien mailto : l'appelant affiche alors l'adresse en clair.
 */
fun openMailApp(context: Context): Boolean =
    try {
        context.startActivity(contactIntent().addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        true
    } catch (_: ActivityNotFoundException) {
        false
    }
