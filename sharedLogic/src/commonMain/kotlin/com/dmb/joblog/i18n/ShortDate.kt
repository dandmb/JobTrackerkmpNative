package com.dmb.joblog.i18n

import kotlinx.datetime.LocalDate
import kotlinx.datetime.format
import kotlinx.datetime.format.MonthNames
import kotlinx.datetime.format.Padding
import kotlinx.datetime.format.char

/**
 * Date courte de la carte de candidature, IDENTIQUE sur Android et iOS (une seule implémentation) :
 * français « 5 sept. » (jour puis mois abrégé), anglais « Sep 5 » (mois abrégé puis jour). Jour sans zéro initial.
 * kotlinx-datetime n'a pas de noms de mois français intégrés : les abréviations françaises sont explicites.
 */
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
