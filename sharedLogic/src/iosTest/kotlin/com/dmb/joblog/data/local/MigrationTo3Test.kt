package com.dmb.joblog.data.local

import androidx.room.testing.MigrationTestHelper
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import androidx.sqlite.execSQL
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

internal class MigrationTo3Test {

    private val databasePath = temporaryDatabasePath()
    private val helper = MigrationTestHelper(
        schemaDirectoryPath = roomSchemaDirectory(),
        fileName = databasePath,
        driver = BundledSQLiteDriver(),
        databaseClass = AppDatabase::class,
        databaseFactory = { AppDatabaseConstructor.initialize() },
    )

    @AfterTest
    fun tearDown() {
        helper.finished()
        deleteDatabaseFiles(databasePath)
    }

    private fun SQLiteConnection.count(table: String): Long = prepare("SELECT COUNT(*) FROM $table").use { it.step(); it.getLong(0) }

    @Test
    fun migration2To3_declaresVersionsTwoAndThree() {
        assertEquals(2, MIGRATION_2_3.startVersion)
        assertEquals(3, MIGRATION_2_3.endVersion)
    }

    @Test
    fun migrate_version2WithApplications_producesTheVersion3Schema_andKeepsEveryRow() {
        helper.createDatabase(2).also {
            it.execSQL(
                "INSERT INTO job_offers (id, title, company, url, location, source, salaryRange, appliedDateEpochDays, " +
                    "interviewDateEpochDays, resultDateEpochDays, status, notes, createdAtEpochMillis) VALUES " +
                    "(1, 'iOS Engineer', 'Atlas Studio', NULL, 'Lyon', 'LinkedIn', '50k - 65k', 20714, 20721, NULL, 'INTERVIEW', 'n', 1000), " +
                    "(2, 'Data Analyst', 'Blue Ocean Labs', NULL, NULL, NULL, NULL, 20712, NULL, NULL, 'APPLIED', NULL, 900)"
            )
            it.close()
        }

        // runMigrationsAndValidate lève une exception si le schéma obtenu diffère de 3.json.
        val connection = helper.runMigrationsAndValidate(3, listOf(MIGRATION_2_3))

        assertEquals(2L, connection.count("job_offers"))
        assertEquals(0L, connection.count("attachments"))
        assertEquals(0L, connection.count("job_offer_attachments"))
        connection.prepare("SELECT title, company, location, salaryRange, interviewDateEpochDays, status, notes FROM job_offers WHERE id = 1").use { row ->
            assertTrue(row.step())
            assertEquals("iOS Engineer", row.getText(0))
            assertEquals("Atlas Studio", row.getText(1))
            assertEquals("Lyon", row.getText(2))
            assertEquals("50k - 65k", row.getText(3))
            assertEquals(20_721L, row.getLong(4))
            assertEquals("INTERVIEW", row.getText(5))
            assertEquals("n", row.getText(6))
            assertFalse(row.step())
        }
        connection.close()
    }

    @Test
    fun migrate_fromVersion1_throughBothMigrations_validatesAgainstVersion3() {
        helper.createDatabase(1).close()

        helper.runMigrationsAndValidate(3, listOf(MIGRATION_1_2, MIGRATION_2_3)).close()
    }
}
