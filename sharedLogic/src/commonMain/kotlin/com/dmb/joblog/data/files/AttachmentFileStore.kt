package com.dmb.joblog.data.files

interface AttachmentFileStore {
    fun newTemporaryPath(): String
    fun sizeOf(path: String): Long
    fun readHeader(path: String, byteCount: Int): ByteArray
    fun commitTemporary(temporaryPath: String, storageName: String)
    fun deleteTemporary(temporaryPath: String)
    fun clearTemporaryFiles()
    fun pathOf(storageName: String): String
    fun exists(storageName: String): Boolean
    fun delete(storageName: String)
    fun storedNames(): List<String>
    fun deleteAll()
}

expect fun createAttachmentFileStore(): AttachmentFileStore
