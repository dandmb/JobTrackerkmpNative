package com.dmb.joblog.ui.joboffer

import androidx.annotation.StringRes
import com.dmb.joblog.R
import com.dmb.joblog.presentation.joboffer.SortOption

/**
 * Ressource du libellé de tri (`sort_*` dans strings.xml), résolue dans la langue courante par l'écran.
 * `SortOption` lui-même vient de `sharedLogic` (comme `ApplicationStatus`) : une seule énumération pour les deux
 * plateformes, chacune ajoutant son propre libellé (voir `SortOption.swift` côté iOS, `titleKey`).
 */
@StringRes
fun SortOption.labelRes(): Int = when (this) {
    SortOption.DATE_DESC -> R.string.sort_newest
    SortOption.DATE_ASC -> R.string.sort_oldest
    SortOption.ALPHA_ASC -> R.string.sort_az
    SortOption.ALPHA_DESC -> R.string.sort_za
}
