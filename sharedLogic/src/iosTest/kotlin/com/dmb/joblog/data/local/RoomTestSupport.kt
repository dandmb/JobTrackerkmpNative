package com.dmb.joblog.data.local

import androidx.room.Room
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.toKString
import kotlinx.coroutines.Dispatchers
import platform.Foundation.NSFileManager
import platform.Foundation.NSTemporaryDirectory
import platform.Foundation.NSUUID
import platform.posix.getenv

/** Base Room en mémoire (SQLite réel via le driver embarqué), sans la migration : version courante du schéma. */
internal fun inMemoryDatabase(): AppDatabase =
    Room.inMemoryDatabaseBuilder<AppDatabase>(factory = { AppDatabaseConstructor.initialize() })
        .setDriver(BundledSQLiteDriver())
        .setQueryCoroutineContext(Dispatchers.Default)
        .build()

/** Dossier des schémas Room exportés (`sharedLogic/schemas`), transmis par Gradle via la variable ROOM_SCHEMA_DIR. */
@OptIn(ExperimentalForeignApi::class)
internal fun roomSchemaDirectory(): String =
    requireNotNull(getenv("ROOM_SCHEMA_DIR")?.toKString()) { "ROOM_SCHEMA_DIR non défini : lancer via Gradle (iosSimulatorArm64Test)" }

/** Chemin d'un fichier de base jetable dans le dossier temporaire du processus de test. */
internal fun temporaryDatabasePath(): String = NSTemporaryDirectory() + "job_offers_test_${NSUUID().UUIDString}.db"

/** Supprime le fichier de base et ses annexes SQLite (-wal, -shm). */
@OptIn(ExperimentalForeignApi::class)
internal fun deleteDatabaseFiles(path: String) {
    listOf(path, "$path-wal", "$path-shm").forEach { NSFileManager.defaultManager.removeItemAtPath(it, error = null) }
}
