package com.dmb.jobtracker.data.local

import androidx.room.migration.Migration
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.execSQL

val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(connection: SQLiteConnection) {
        connection.execSQL("ALTER TABLE job_offers ADD COLUMN location TEXT")
        connection.execSQL("ALTER TABLE job_offers ADD COLUMN source TEXT")
        connection.execSQL("ALTER TABLE job_offers ADD COLUMN salaryRange TEXT")
        connection.execSQL("ALTER TABLE job_offers ADD COLUMN interviewDateEpochDays INTEGER")
        connection.execSQL("ALTER TABLE job_offers ADD COLUMN resultDateEpochDays INTEGER")
    }
}