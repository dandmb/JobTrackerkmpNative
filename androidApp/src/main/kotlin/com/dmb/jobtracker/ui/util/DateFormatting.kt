package com.dmb.jobtracker.ui.util

import kotlinx.datetime.LocalDate
import kotlinx.datetime.format
import kotlinx.datetime.format.MonthNames
import kotlinx.datetime.format.Padding
import kotlinx.datetime.format.char

// kotlinx-datetime n'a pas de noms de mois français intégrés : mêmes abréviations que iOS (fr_FR, « d MMM »).
private val FrenchMonthsAbbreviated = MonthNames(
    "janv.", "févr.", "mars", "avr.", "mai", "juin",
    "juil.", "août", "sept.", "oct.", "nov.", "déc."
)

private val ShortFrenchDateFormat = LocalDate.Format {
    dayOfMonth(Padding.NONE)
    char(' ')
    monthName(FrenchMonthsAbbreviated)
}

/** « 5 sept. », « 12 févr. » : jour sans zéro initial + mois abrégé en français (identique à iOS). */
fun LocalDate.toShortFrenchDate(): String = format(ShortFrenchDateFormat)
