package com.dmb.joblog.testutil

import com.dmb.joblog.data.files.AttachmentFileStore

internal class FakeAttachmentFileStore : AttachmentFileStore {

    val temporary = mutableMapOf<String, ByteArray>()
    val stored = mutableMapOf<String, ByteArray>()
    var deleteAllCalls = 0
        private set
    private var nextTemporary = 1

    fun writeTemporary(bytes: ByteArray): String = newTemporaryPath().also { temporary[it] = bytes }

    override fun newTemporaryPath(): String = "tmp/${nextTemporary++}"

    override fun sizeOf(path: String): Long = (temporary[path]?.size ?: 0).toLong()

    override fun readHeader(path: String, byteCount: Int): ByteArray = temporary[path]?.take(byteCount)?.toByteArray() ?: ByteArray(0)

    override fun commitTemporary(temporaryPath: String, storageName: String) {
        stored[storageName] = checkNotNull(temporary.remove(temporaryPath))
    }

    override fun deleteTemporary(temporaryPath: String) {
        temporary.remove(temporaryPath)
    }

    override fun clearTemporaryFiles() = temporary.clear()

    override fun pathOf(storageName: String): String = "files/$storageName"

    override fun exists(storageName: String): Boolean = storageName in stored

    override fun delete(storageName: String) {
        stored.remove(storageName)
    }

    override fun storedNames(): List<String> = stored.keys.toList()

    override fun deleteAll() {
        deleteAllCalls++
        stored.clear()
        temporary.clear()
    }
}
