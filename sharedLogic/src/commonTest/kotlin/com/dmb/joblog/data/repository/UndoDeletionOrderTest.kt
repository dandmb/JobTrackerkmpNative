package com.dmb.joblog.data.repository

import com.dmb.joblog.domain.usecase.DeleteJobOfferUseCase
import com.dmb.joblog.testutil.FakeJobOfferDao
import com.dmb.joblog.testutil.jobOffer
import com.dmb.joblog.testutil.jobOfferEntity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals

/**
 * « Annuler » une suppression : la candidature doit retrouver EXACTEMENT sa position dans la liste triée par date de création
 * décroissante, avec tous ses champs (dont `createdAt`) inchangés. Repository de production sur DAO fake qui reproduit le tri.
 */
class UndoDeletionOrderTest {

    private val dao = FakeJobOfferDao()
    private val repository = JobOfferRepositoryImpl(dao)
    private val deleteJobOffer = DeleteJobOfferUseCase(repository)

    /** 5 offres A..E, de la plus récente (A) à la plus ancienne (E) : ids 1..5, createdAt 5000..1000. */
    private val originals = listOf("A", "B", "C", "D", "E").mapIndexed { i, title ->
        jobOfferEntity(
            id = (i + 1).toLong(), title = title, company = "Société $title", location = "Ville $title", source = "Source $title",
            salaryRange = "${40 + i}k - ${50 + i}k", url = "https://exemple.fr/$title", notes = "Notes $title",
            interviewDateEpochDays = 20_700L + i, resultDateEpochDays = 20_710L + i, createdAtEpochMillis = (5 - i) * 1_000L,
        )
    }

    private fun seed() { dao.entities.value = originals }
    private suspend fun titles() = repository.getAll().first().map { it.title }
    private fun offerOf(title: String) = originals.first { it.title == title }.let { e ->
        jobOffer(id = e.id, title = e.title, company = e.company)
    }

    @Test
    fun undo_ofAMiddleOffer_putsItBackAtItsOriginalPositionNotAtTheTop() = runTest {
        seed()
        val deleted = deleteJobOffer(repository.getAll().first().first { it.title == "C" })
        assertEquals(listOf("A", "B", "D", "E"), titles())

        deleteJobOffer.restore(deleted)

        assertEquals(listOf("A", "B", "C", "D", "E"), titles())
    }

    @Test
    fun undo_restoresEveryStoredFieldIncludingCreatedAt() = runTest {
        seed()
        val target = originals.first { it.title == "C" }
        val deleted = deleteJobOffer(repository.getAll().first().first { it.id == target.id })

        deleteJobOffer.restore(deleted)

        assertEquals(target, dao.entities.value.first { it.id == target.id }, "l'entité restaurée est identique à l'originale (createdAt compris)")
    }

    @Test
    fun undo_ofTheFirstAndOfTheLastOffer_keepsThemAtTheTopAndAtTheBottom() = runTest {
        seed()
        val all = repository.getAll().first()
        val a = deleteJobOffer(all.first { it.title == "A" })
        val e = deleteJobOffer(all.first { it.title == "E" })

        deleteJobOffer.restore(e)
        deleteJobOffer.restore(a)

        assertEquals(listOf("A", "B", "C", "D", "E"), titles())
    }

    @Test
    fun severalDeletions_thenUndoInADifferentOrder_endWithTheOriginalOrder() = runTest {
        seed()
        val all = repository.getAll().first()
        val b = deleteJobOffer(all.first { it.title == "B" })
        val d = deleteJobOffer(all.first { it.title == "D" })
        val a = deleteJobOffer(all.first { it.title == "A" })
        assertEquals(listOf("C", "E"), titles())

        deleteJobOffer.restore(d)   // ordre d'annulation ≠ ordre de suppression
        assertEquals(listOf("C", "D", "E"), titles())
        deleteJobOffer.restore(a)
        assertEquals(listOf("A", "C", "D", "E"), titles())
        deleteJobOffer.restore(b)

        assertEquals(listOf("A", "B", "C", "D", "E"), titles())
        assertEquals(originals.toSet(), dao.entities.value.toSet(), "toutes les entités sont identiques aux originales")
    }

    @Test
    fun undo_ofOnlySomeOfTheDeletions_leavesTheOthersDeleted() = runTest {
        seed()
        val all = repository.getAll().first()
        deleteJobOffer(all.first { it.title == "B" })
        val d = deleteJobOffer(all.first { it.title == "D" })

        deleteJobOffer.restore(d)

        assertEquals(listOf("A", "C", "D", "E"), titles())
    }

    @Test
    fun aPlainAddAfterDelete_wouldHaveMovedItToTheTop_provingTheTestsCatchTheOriginalBug() = runTest {
        seed()
        val c = repository.getAll().first().first { it.title == "C" }
        repository.delete(c)

        repository.add(c)   // ancien comportement de « Annuler » : createdAt = maintenant

        assertEquals("C", titles().first(), "ancien défaut : la candidature remonte en tête")
        assertNotEquals(3_000L, dao.entities.value.first { it.id == c.id }.createdAtEpochMillis)
    }

    @Test
    fun restore_ofAnOfferStillPresent_replacesItWithoutDuplicatingIt() = runTest {
        seed()
        val c = repository.getAll().first().first { it.title == "C" }

        repository.restore(c, 3_000L)

        assertEquals(listOf("A", "B", "C", "D", "E"), titles())
        assertEquals(5, dao.entities.value.size)
    }

    @Test
    fun getCreatedAt_returnsTheStoredValue_andNullForAnUnknownId() = runTest {
        seed()

        assertEquals(3_000L, repository.getCreatedAt(3))
        assertEquals(null, repository.getCreatedAt(99))
    }
}
