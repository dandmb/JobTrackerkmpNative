package com.dmb.joblog.data.local

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
// SQL recopié de schemas/…/3.json : Room valide le schéma migré contre ce fichier, toute différence fait planter l'ouverture.
val MIGRATION_2_3 = object : Migration(2, 3) {
    override fun migrate(connection: SQLiteConnection) {
        connection.execSQL(
            "CREATE TABLE IF NOT EXISTS `attachments` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `displayName` TEXT NOT NULL, " +
                "`kind` TEXT NOT NULL, `format` TEXT NOT NULL, `sizeBytes` INTEGER NOT NULL, `addedAtEpochMillis` INTEGER NOT NULL, " +
                "`inLibrary` INTEGER NOT NULL, `storageName` TEXT NOT NULL)"
        )
        connection.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_attachments_storageName` ON `attachments` (`storageName`)")
        connection.execSQL(
            "CREATE TABLE IF NOT EXISTS `job_offer_attachments` (`jobOfferId` INTEGER NOT NULL, `role` TEXT NOT NULL, " +
                "`attachmentId` INTEGER NOT NULL, PRIMARY KEY(`jobOfferId`, `role`), " +
                "FOREIGN KEY(`jobOfferId`) REFERENCES `job_offers`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE , " +
                "FOREIGN KEY(`attachmentId`) REFERENCES `attachments`(`id`) ON UPDATE NO ACTION ON DELETE RESTRICT )"
        )
        connection.execSQL("CREATE INDEX IF NOT EXISTS `index_job_offer_attachments_attachmentId` ON `job_offer_attachments` (`attachmentId`)")
    }
}
