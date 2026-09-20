package com.dmb.jobtracker.data.local

import androidx.room.Room
import androidx.room.testing.MigrationTestHelper
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import androidx.sqlite.execSQL
import com.dmb.jobtracker.data.repository.JobOfferRepositoryImpl
import com.dmb.jobtracker.domain.model.ApplicationStatus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDate
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFails
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Migration 1 → 2 (ajout de location, source, salaryRange, interviewDateEpochDays, resultDateEpochDays) sur un FICHIER
 * SQLite réel, avec l'outil officiel `MigrationTestHelper` (room-testing 2.8.5, multiplatform) qui crée la base à partir
 * du schéma exporté `schemas/.../1.json` puis valide le résultat contre `2.json`.
 */
internal class MigrationTest {

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

    private fun SQLiteConnection.insertV1(id: Long, title: String, company: String = "Acme", url: String? = null, notes: String? = null) {
        fun sql(text: String?) = if (text == null) "NULL" else "'${text.replace("'", "''")}'"
        execSQL(
            "INSERT INTO job_offers (id, title, company, url, appliedDateEpochDays, status, notes, createdAtEpochMillis) " +
                "VALUES ($id, ${sql(title)}, ${sql(company)}, ${sql(url)}, 20701, 'APPLIED', ${sql(notes)}, ${1_000 + id})",
        )
    }

    private fun migratedConnection() = helper.runMigrationsAndValidate(2, listOf(MIGRATION_1_2))

    // ---------- la migration elle-même ----------

    @Test
    fun migration1To2_declaresVersionsOneAndTwo() {
        assertEquals(1, MIGRATION_1_2.startVersion)
        assertEquals(2, MIGRATION_1_2.endVersion)
    }

    @Test
    fun migrate_emptyVersion1Database_producesTheVersion2SchemaValidatedAgainst2Json() {
        helper.createDatabase(1).close()

        // runMigrationsAndValidate lève IllegalStateException si le schéma obtenu diffère de 2.json.
        migratedConnection().close()
    }

    @Test
    fun migrate_version1RowsWithData_arePreservedIntact() {
        helper.createDatabase(1).also {
            it.insertV1(id = 1, title = "Dev", company = "Acme", url = "https://a.io", notes = "à relancer")
            it.insertV1(id = 2, title = "Lead", company = "Zeta")
            it.close()
        }

        val connection = migratedConnection()

        connection.prepare("SELECT id, title, company, url, appliedDateEpochDays, status, notes, createdAtEpochMillis FROM job_offers ORDER BY id").use { row ->
            assertTrue(row.step())
            assertEquals(1L, row.getLong(0))
            assertEquals("Dev", row.getText(1))
            assertEquals("Acme", row.getText(2))
            assertEquals("https://a.io", row.getText(3))
            assertEquals(20_701L, row.getLong(4))
            assertEquals("APPLIED", row.getText(5))
            assertEquals("à relancer", row.getText(6))
            assertEquals(1_001L, row.getLong(7))
            assertTrue(row.step())
            assertEquals("Lead", row.getText(1))
            assertTrue(row.isNull(3), "url absente en v1 → reste NULL")
            assertFalse(row.step())
        }
        connection.close()
    }

    @Test
    fun migrate_version1Rows_getNullInEveryAddedColumn() {
        helper.createDatabase(1).also { it.insertV1(id = 1, title = "Dev"); it.close() }

        val connection = migratedConnection()

        connection.prepare("SELECT location, source, salaryRange, interviewDateEpochDays, resultDateEpochDays FROM job_offers").use { row ->
            assertTrue(row.step())
            (0..4).forEach { assertTrue(row.isNull(it), "colonne ajoutée n°$it doit être NULL après migration") }
        }
        connection.close()
    }

    @Test
    fun migrate_manyVersion1Rows_areAllKept() {
        helper.createDatabase(1).also { c -> (1L..25L).forEach { c.insertV1(id = it, title = "Offre $it") }; c.close() }

        val connection = migratedConnection()

        connection.prepare("SELECT COUNT(*) FROM job_offers").use { row ->
            assertTrue(row.step())
            assertEquals(25L, row.getLong(0))
        }
        connection.close()
    }

    @Test
    fun migrate_titleWithQuoteAndAccents_isPreserved() {
        helper.createDatabase(1).also { it.insertV1(id = 1, title = "Ingénieur d'études « senior »"); it.close() }

        val connection = migratedConnection()

        connection.prepare("SELECT title FROM job_offers").use { row ->
            assertTrue(row.step())
            assertEquals("Ingénieur d'études « senior »", row.getText(0))
        }
        connection.close()
    }

    @Test
    fun migrate_thenInsertUsingTheNewColumns_works() {
        helper.createDatabase(1).close()
        val connection = migratedConnection()

        connection.execSQL(
            "INSERT INTO job_offers (title, company, location, source, salaryRange, appliedDateEpochDays, interviewDateEpochDays, " +
                "resultDateEpochDays, status, createdAtEpochMillis) VALUES ('Dev', 'Acme', 'Paris', 'LinkedIn', '55k - 70k', 20701, 20708, 20715, 'INTERVIEW', 5)",
        )

        connection.prepare("SELECT location, source, salaryRange, interviewDateEpochDays, resultDateEpochDays FROM job_offers").use { row ->
            assertTrue(row.step())
            assertEquals("Paris", row.getText(0))
            assertEquals("LinkedIn", row.getText(1))
            assertEquals("55k - 70k", row.getText(2))
            assertEquals(20_708L, row.getLong(3))
            assertEquals(20_715L, row.getLong(4))
        }
        connection.close()
    }

    @Test
    fun migrate_withoutAnyMigrationProvided_fails() {
        helper.createDatabase(1).close()

        assertFails { helper.runMigrationsAndValidate(2, emptyList()) }
    }

    // ---------- de bout en bout : ouverture par le vrai code de l'app ----------

    @Test
    fun openWithProductionBuilder_version1Database_isMigratedAndReadableThroughTheRepository() = runTest {
        helper.createDatabase(1).also {
            it.insertV1(id = 1, title = "Dev iOS", company = "eBay", notes = "n")
            it.close()
        }
        helper.finished()   // libère le fichier avant de le rouvrir avec Room

        // getRoomDatabase = le code de production (ajoute MIGRATION_1_2 et le driver embarqué)
        val database = getRoomDatabase(
            Room.databaseBuilder<AppDatabase>(name = databasePath, factory = { AppDatabaseConstructor.initialize() })
                .setQueryCoroutineContext(Dispatchers.Default),
        )
        try {
            val offers = JobOfferRepositoryImpl(database.jobOfferDao()).getAll().first()

            val offer = offers.single()
            assertEquals("Dev iOS", offer.title)
            assertEquals("eBay", offer.company)
            assertEquals(LocalDate(2026, 9, 5), offer.appliedDate)
            assertEquals(ApplicationStatus.APPLIED, offer.status)
            assertEquals("n", offer.notes)
            assertNull(offer.location)
            assertNull(offer.source)
            assertNull(offer.salaryRange)
            assertNull(offer.interviewDate)
            assertNull(offer.resultDate)
        } finally {
            database.close()
        }
    }
}
