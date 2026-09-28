package com.dmb.joblog.ui.util

import com.dmb.joblog.i18n.AppLanguage
import com.dmb.joblog.i18n.ShortDate
import kotlinx.datetime.LocalDate

fun LocalDate.toShortDate(language: AppLanguage): String = ShortDate.format(this, language)
