package com.dmb.jobtracker.data.local.converter

import androidx.room.TypeConverter
import com.dmb.jobtracker.domain.model.ApplicationStatus

class Converters {
    @TypeConverter
    fun fromStatus(status: ApplicationStatus): String = status.name

    @TypeConverter
    fun toStatus(value: String): ApplicationStatus = ApplicationStatus.valueOf(value)
}