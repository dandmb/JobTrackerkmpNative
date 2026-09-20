package com.dmb.jobtracker.ui.joboffer

import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.toLocalDateTime

// Spécifique à Android : le DatePicker Material travaille en millisecondes UTC à minuit. (Toute la logique de formulaire
// commune — salaire, validation, assemblage de l'offre — est dans sharedLogic : `presentation.form.JobOfferFormLogic`.)

fun LocalDate.toDatePickerMillis(): Long = atStartOfDayIn(TimeZone.UTC).toEpochMilliseconds()

fun datePickerMillisToLocalDate(millis: Long): LocalDate =
    Instant.fromEpochMilliseconds(millis).toLocalDateTime(TimeZone.UTC).date
