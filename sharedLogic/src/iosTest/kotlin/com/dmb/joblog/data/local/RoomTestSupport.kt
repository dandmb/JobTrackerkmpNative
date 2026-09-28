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

internal fun inMemoryDatabase(): AppDatabase =
    Room.inMemoryDatabaseBuilder<AppDatabase>(factory = { AppDatabaseConstructor.initialize() })
        .setDriver(BundledSQLiteDriver())
        .setQueryCoroutineContext(Dispatchers.Default)
        .build()

@OptIn(ExperimentalForeignApi::class)
internal fun roomSchemaDirectory(): String =
    requireNotNull(getenv("ROOM_SCHEMA_DIR")?.toKString()) { "ROOM_SCHEMA_DIR non défini : lancer via Gradle (iosSimulatorArm64Test)" }

internal fun temporaryDatabasePath(): String = NSTemporaryDirectory() + "job_offers_test_${NSUUID().UUIDString}.db"

@OptIn(ExperimentalForeignApi::class)
internal fun deleteDatabaseFiles(path: String) {
    listOf(path, "$path-wal", "$path-shm").forEach { NSFileManager.defaultManager.removeItemAtPath(it, error = null) }
}
