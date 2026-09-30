package com.dmb.joblog.testutil

import com.dmb.joblog.data.local.dao.AttachmentDao
import com.dmb.joblog.data.local.entity.AttachmentEntity
import com.dmb.joblog.data.local.entity.AttachmentWithUsage
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine

internal class FakeAttachmentDao(private val usageByAttachmentId: MutableStateFlow<Map<Long, Int>> = MutableStateFlow(emptyMap())) : AttachmentDao {

    val entities = MutableStateFlow<List<AttachmentEntity>>(emptyList())
    var failNextInsert = false
    private var nextId = 1L

    fun setUsage(attachmentId: Long, count: Int) {
        usageByAttachmentId.value = usageByAttachmentId.value + (attachmentId to count)
    }

    override suspend fun insert(attachment: AttachmentEntity): Long {
        if (failNextInsert) {
            failNextInsert = false
            error("insert failed")
        }
        val id = nextId++
        entities.value = entities.value + attachment.copy(id = id)
        return id
    }

    override fun getAllWithUsage(): Flow<List<AttachmentWithUsage>> =
        combine(entities, usageByAttachmentId) { list, usage ->
            list.sortedWith(compareByDescending<AttachmentEntity> { it.addedAtEpochMillis }.thenByDescending { it.id })
                .map { AttachmentWithUsage(it, usage[it.id] ?: 0) }
        }

    override suspend fun getById(id: Long): AttachmentEntity? = entities.value.firstOrNull { it.id == id }

    override suspend fun usageCount(id: Long): Int = usageByAttachmentId.value[id] ?: 0

    override suspend fun rename(id: Long, displayName: String) {
        entities.value = entities.value.map { if (it.id == id) it.copy(displayName = displayName) else it }
    }

    override suspend fun deleteById(id: Long) {
        entities.value = entities.value.filterNot { it.id == id }
    }

    override suspend fun getOrphanOneTimeAttachments(): List<AttachmentEntity> =
        entities.value.filter { !it.inLibrary && (usageByAttachmentId.value[it.id] ?: 0) == 0 }

    override suspend fun getAllStorageNames(): List<String> = entities.value.map { it.storageName }
}
