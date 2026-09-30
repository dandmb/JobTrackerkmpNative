package com.dmb.joblog.data.files

import kotlinx.cinterop.BetaInteropApi
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.usePinned
import platform.Foundation.NSData
import platform.Foundation.NSFileManager
import platform.Foundation.NSNumber
import platform.Foundation.NSTemporaryDirectory
import platform.Foundation.NSURL
import platform.Foundation.NSURLIsExcludedFromBackupKey
import platform.Foundation.NSUUID
import platform.Foundation.create
import platform.Foundation.writeToFile
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@OptIn(ExperimentalForeignApi::class, BetaInteropApi::class)
internal fun writeBytes(path: String, bytes: ByteArray) {
    val data = bytes.usePinned { NSData.create(bytes = it.addressOf(0), length = bytes.size.toULong()) }
    check(data.writeToFile(path, atomically = true))
}

@OptIn(ExperimentalForeignApi::class)
internal class DirectoryAttachmentFileStoreTest {

    private val root = NSTemporaryDirectory() + "attachments_test_${NSUUID().UUIDString}"
    private val store = DirectoryAttachmentFileStore(root)
    private val fileManager = NSFileManager.defaultManager

    @AfterTest
    fun tearDown() {
        fileManager.removeItemAtPath(root, error = null)
        fileManager.removeItemAtPath(shareDirectory, error = null)
    }

    private val shareDirectory = NSTemporaryDirectory() + "share_test_${NSUUID().UUIDString}"

    private fun storeWithShareCopy(): DirectoryAttachmentFileStore {
        fileManager.createDirectoryAtPath(shareDirectory, withIntermediateDirectories = true, attributes = null, error = null)
        writeBytes("$shareDirectory/CV.pdf", byteArrayOf(1))
        return DirectoryAttachmentFileStore(root, shareDirectory)
    }

    @Test
    fun temporaryCopy_isCommittedUnderItsStorageName() {
        val temporary = store.newTemporaryPath()
        writeBytes(temporary, "%PDF-1.7 hello".encodeToByteArray())

        assertEquals(14L, store.sizeOf(temporary))
        assertContentEquals("%PDF".encodeToByteArray(), store.readHeader(temporary, 4))
        store.commitTemporary(temporary, "abc.pdf")

        assertTrue(store.exists("abc.pdf"))
        assertFalse(fileManager.fileExistsAtPath(temporary))
        assertEquals(listOf("abc.pdf"), store.storedNames())
        assertEquals("$root/abc.pdf", store.pathOf("abc.pdf"))
    }

    @Test
    fun rootDirectory_isExcludedFromBackup() {
        store.newTemporaryPath()

        val url = NSURL.fileURLWithPath(root)
        val values = url.resourceValuesForKeys(listOf(NSURLIsExcludedFromBackupKey), error = null)
        assertEquals(true, (values?.get(NSURLIsExcludedFromBackupKey) as? NSNumber)?.boolValue)
    }

    @Test
    fun deleteAndClear_removeOnlyWhatTheyShould() {
        val a = store.newTemporaryPath().also { writeBytes(it, byteArrayOf(1)) }
        store.commitTemporary(a, "a.pdf")
        val b = store.newTemporaryPath().also { writeBytes(it, byteArrayOf(2)) }
        store.commitTemporary(b, "b.pdf")
        val pending = store.newTemporaryPath().also { writeBytes(it, byteArrayOf(3)) }

        store.delete("a.pdf")
        store.clearTemporaryFiles()

        assertFalse(store.exists("a.pdf"))
        assertTrue(store.exists("b.pdf"))
        assertFalse(fileManager.fileExistsAtPath(pending))
        assertEquals(listOf("b.pdf"), store.storedNames())
    }

    @Test
    fun deleteAll_removesTheWholeDirectoryFromDisk() {
        val a = store.newTemporaryPath().also { writeBytes(it, byteArrayOf(1)) }
        store.commitTemporary(a, "a.pdf")
        store.newTemporaryPath().also { writeBytes(it, byteArrayOf(2)) }

        store.deleteAll()

        assertFalse(fileManager.fileExistsAtPath(root))
        assertTrue(store.storedNames().isEmpty())
    }

    @Test
    fun startupCleanUpAndDeleteAll_bothRemoveTheShareCopy() {
        storeWithShareCopy().clearTemporaryFiles()
        assertFalse(fileManager.fileExistsAtPath(shareDirectory))

        storeWithShareCopy().deleteAll()
        assertFalse(fileManager.fileExistsAtPath(shareDirectory))
    }

    @Test
    fun shareDirectory_isInTheSystemTemporaryDirectory() {
        assertTrue(attachmentShareDirectoryPath().startsWith(NSTemporaryDirectory()))
        assertTrue(attachmentShareDirectoryPath().endsWith("/shared_attachments"))
    }
}
