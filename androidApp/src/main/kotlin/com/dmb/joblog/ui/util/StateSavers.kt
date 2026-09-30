package com.dmb.joblog.ui.util

import androidx.compose.runtime.saveable.Saver
import com.dmb.joblog.domain.model.ApplicationStatus
import kotlinx.datetime.LocalDate

val LocalDateSaver: Saver<LocalDate, Int> = Saver(
    save = { it.toEpochDays() },
    restore = { LocalDate.fromEpochDays(it) },
)

val OptionalLocalDateSaver: Saver<LocalDate?, Int> = Saver(
    save = { it?.toEpochDays() },
    restore = { LocalDate.fromEpochDays(it) },
)

val StatusSetSaver: Saver<Set<ApplicationStatus>, ArrayList<String>> = Saver(
    save = { statuses -> ArrayList(statuses.map { it.name }) },
    restore = { names -> names.map { ApplicationStatus.valueOf(it) }.toSet() },
)
