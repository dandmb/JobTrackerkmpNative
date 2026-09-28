package com.dmb.joblog.data.local.converter

import androidx.room.TypeConverter
import com.dmb.joblog.domain.model.ApplicationStatus

class Converters {
    @TypeConverter
    fun fromStatus(status: ApplicationStatus): String = status.name

    @TypeConverter
    fun toStatus(value: String): ApplicationStatus = ApplicationStatus.valueOf(value)
}