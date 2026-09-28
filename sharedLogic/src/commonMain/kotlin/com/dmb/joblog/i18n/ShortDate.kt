package com.dmb.joblog.i18n

import kotlinx.datetime.LocalDate
import kotlinx.datetime.format
import kotlinx.datetime.format.MonthNames
import kotlinx.datetime.format.Padding
import kotlinx.datetime.format.char

object ShortDate {

    private val frenchMonths = MonthNames(
        "janv.", "févr.", "mars", "avr.", "mai", "juin",
        "juil.", "août", "sept.", "oct.", "nov.", "déc.",
    )

    private val french = LocalDate.Format {
        dayOfMonth(Padding.NONE)
        char(' ')
        monthName(frenchMonths)
    }

    private val english = LocalDate.Format {
        monthName(MonthNames.ENGLISH_ABBREVIATED)
        char(' ')
        dayOfMonth(Padding.NONE)
    }

    fun format(date: LocalDate, language: AppLanguage): String =
        date.format(if (language == AppLanguage.FR) french else english)
}
