package com.dmb.joblog.ui.util

import com.dmb.joblog.i18n.AppLanguage
import com.dmb.joblog.i18n.ShortDate
import kotlinx.datetime.LocalDate

/** « 5 sept. » (fr) / « Sep 5 » (en) : format court de la carte, identique à iOS (une seule implémentation, sharedLogic). */
fun LocalDate.toShortDate(language: AppLanguage): String = ShortDate.format(this, language)
