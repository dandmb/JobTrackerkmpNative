package com.dmb.joblog.ui.joboffer

import androidx.annotation.StringRes
import com.dmb.joblog.R

/** Options de tri ; le libellé est une ressource (`sort_*` dans strings.xml), résolue dans la langue courante par l'écran. */
enum class SortOption(@StringRes val labelRes: Int) {
    DATE_DESC(R.string.sort_newest),
    DATE_ASC(R.string.sort_oldest),
    ALPHA_ASC(R.string.sort_az),
    ALPHA_DESC(R.string.sort_za)
}
