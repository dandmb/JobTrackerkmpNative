package com.dmb.joblog.data.files

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.readBytes
import platform.Foundation.NSApplicationSupportDirectory
import platform.Foundation.NSFileHandle
import platform.Foundation.NSFileManager
import platform.Foundation.NSFileSize
import platform.Foundation.NSNumber
import platform.Foundation.NSTemporaryDirectory
import platform.Foundation.NSURL
import platform.Foundation.NSURLIsExcludedFromBackupKey
import platform.Foundation.NSUserDomainMask
import platform.Foundation.fileHandleForReadingAtPath
import platform.Foundation.readDataOfLength
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

@OptIn(ExperimentalForeignApi::class)
internal class DirectoryAttachmentFileStore(
    private val rootPath: String,
    private val shareDirectoryPath: String? = null,
) : AttachmentFileStore {

    private val fileManager get() = NSFileManager.defaultManager
    private val temporaryPath get() = "$rootPath/$TEMPORARY_DIRECTORY"

    private fun ensureDirectories() {
        fileManager.createDirectoryAtPath(temporaryPath, withIntermediateDirectories = true, attributes = null, error = null)
        NSURL.fileURLWithPath(rootPath).setResourceValue(true, forKey = NSURLIsExcludedFromBackupKey, error = null)
    }

    @OptIn(ExperimentalUuidApi::class)
    override fun newTemporaryPath(): String {
        ensureDirectories()
        return "$temporaryPath/${Uuid.random()}"
    }

    override fun sizeOf(path: String): Long =
        (fileManager.attributesOfItemAtPath(path, error = null)?.get(NSFileSize) as? NSNumber)?.longLongValue ?: 0L

    override fun readHeader(path: String, byteCount: Int): ByteArray {
        val handle = NSFileHandle.fileHandleForReadingAtPath(path) ?: return ByteArray(0)
        val data = handle.readDataOfLength(byteCount.toULong())
        handle.closeAndReturnError(null)
        val length = data.length.toInt()
        return if (length == 0) ByteArray(0) else data.bytes?.readBytes(length) ?: ByteArray(0)
    }

    override fun commitTemporary(temporaryPath: String, storageName: String) {
        ensureDirectories()
        check(fileManager.moveItemAtPath(temporaryPath, toPath = pathOf(storageName), error = null)) { "move failed" }
    }

    override fun deleteTemporary(temporaryPath: String) {
        fileManager.removeItemAtPath(temporaryPath, error = null)
    }

    override fun clearTemporaryFiles() {
        fileManager.removeItemAtPath(temporaryPath, error = null)
        shareDirectoryPath?.let { fileManager.removeItemAtPath(it, error = null) }
    }

    override fun pathOf(storageName: String): String = "$rootPath/$storageName"

    override fun exists(storageName: String): Boolean = fileManager.fileExistsAtPath(pathOf(storageName))

    override fun delete(storageName: String) {
        fileManager.removeItemAtPath(pathOf(storageName), error = null)
    }

    override fun storedNames(): List<String> =
        fileManager.contentsOfDirectoryAtPath(rootPath, error = null)?.filterIsInstance<String>()?.filter { it != TEMPORARY_DIRECTORY }.orEmpty()

    override fun deleteAll() {
        fileManager.removeItemAtPath(rootPath, error = null)
        shareDirectoryPath?.let { fileManager.removeItemAtPath(it, error = null) }
    }

    private companion object {
        const val TEMPORARY_DIRECTORY = "tmp"
    }
}

@OptIn(ExperimentalForeignApi::class)
actual fun createAttachmentFileStore(): AttachmentFileStore {
    val applicationSupport = NSFileManager.defaultManager.URLForDirectory(
        directory = NSApplicationSupportDirectory,
        inDomain = NSUserDomainMask,
        appropriateForURL = null,
        create = true,
        error = null,
    )
    return DirectoryAttachmentFileStore(
        rootPath = requireNotNull(applicationSupport).path + "/Attachments",
        shareDirectoryPath = attachmentShareDirectoryPath(),
    )
}

// QuickLook affiche le nom du fichier et la feuille de partage le transmet tel quel : on présente une copie nommée
// d'après le nom affiché (pas `<uuid>.<ext>`), effacée au prochain aperçu, au nettoyage de démarrage et par la suppression totale.
fun attachmentShareDirectoryPath(): String = NSTemporaryDirectory() + "shared_attachments"
