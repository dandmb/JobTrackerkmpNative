package com.dmb.joblog.data.files

import com.dmb.joblog.data.local.AndroidAppContextHolder
import java.io.File
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

internal class DirectoryAttachmentFileStore(
    private val root: File,
    private val shareDirectory: File? = null,
) : AttachmentFileStore {

    private val temporaryDirectory: File get() = File(root, TEMPORARY_DIRECTORY)

    @OptIn(ExperimentalUuidApi::class)
    override fun newTemporaryPath(): String {
        temporaryDirectory.mkdirs()
        return File(temporaryDirectory, Uuid.random().toString()).absolutePath
    }

    override fun sizeOf(path: String): Long = File(path).length()

    override fun readHeader(path: String, byteCount: Int): ByteArray =
        File(path).inputStream().use { input ->
            val buffer = ByteArray(byteCount)
            val read = input.read(buffer)
            if (read <= 0) ByteArray(0) else buffer.copyOf(read)
        }

    override fun commitTemporary(temporaryPath: String, storageName: String) {
        root.mkdirs()
        val source = File(temporaryPath)
        val target = File(root, storageName)
        if (!source.renameTo(target)) {
            source.copyTo(target, overwrite = false)
            source.delete()
        }
    }

    override fun deleteTemporary(temporaryPath: String) {
        File(temporaryPath).delete()
    }

    override fun clearTemporaryFiles() {
        temporaryDirectory.deleteRecursively()
        shareDirectory?.deleteRecursively()
    }

    override fun pathOf(storageName: String): String = File(root, storageName).absolutePath

    override fun exists(storageName: String): Boolean = File(root, storageName).isFile

    override fun delete(storageName: String) {
        File(root, storageName).delete()
    }

    override fun storedNames(): List<String> = root.listFiles()?.filter { it.isFile }?.map { it.name }.orEmpty()

    override fun deleteAll() {
        root.deleteRecursively()
        shareDirectory?.deleteRecursively()
    }

    private companion object {
        const val TEMPORARY_DIRECTORY = "tmp"
    }
}

// noBackupFilesDir : exclu de la sauvegarde Google (quota de 25 Mo par app, au-delà plus RIEN n'est sauvegardé, base comprise).
actual fun createAttachmentFileStore(): AttachmentFileStore = DirectoryAttachmentFileStore(
    root = File(AndroidAppContextHolder.context.noBackupFilesDir, "attachments"),
    shareDirectory = attachmentShareDirectory(),
)

// FileProvider ne sait pas exposer noBackupFilesDir : « Ouvrir avec… » partage une copie placée ici (cache, déclaré dans
// res/xml/attachment_paths.xml de l'app), effacée au prochain partage, au nettoyage de démarrage et par la suppression totale.
fun attachmentShareDirectory(): File = File(AndroidAppContextHolder.context.cacheDir, "shared_attachments")
