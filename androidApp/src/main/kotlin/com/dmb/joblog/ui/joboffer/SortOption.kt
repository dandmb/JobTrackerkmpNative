package com.dmb.joblog.ui.joboffer

import androidx.annotation.StringRes
import com.dmb.joblog.R
import com.dmb.joblog.presentation.joboffer.SortOption

@StringRes
fun SortOption.labelRes(): Int = when (this) {
    SortOption.DATE_DESC -> R.string.sort_newest
    SortOption.DATE_ASC -> R.string.sort_oldest
    SortOption.ALPHA_ASC -> R.string.sort_az
    SortOption.ALPHA_DESC -> R.string.sort_za
}
