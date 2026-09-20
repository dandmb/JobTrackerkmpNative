package com.dmb.jobtracker.data.mapper

import com.dmb.jobtracker.domain.model.ApplicationStatus
import com.dmb.jobtracker.testutil.jobOffer
import com.dmb.jobtracker.testutil.jobOfferEntity
import kotlinx.datetime.Clock
import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class JobOfferMapperTest {

    // ---------- toDomain ----------

    @Test
    fun toDomain_fullEntity_mapsEveryField() {
        val entity = jobOfferEntity(
            id = 12, title = "Dev", company = "Acme", url = "https://acme.io/job", location = "Paris",
            source = "LinkedIn", salaryRange = "50-60", appliedDateEpochDays = 20_701,
            interviewDateEpochDays = 20_708, resultDateEpochDays = 20_715,
            status = ApplicationStatus.INTERVIEW, notes = "à relancer", createdAtEpochMillis = 999,
        )

        val domain = entity.toDomain()

        assertEquals(12L, domain.id)
        assertEquals("Dev", domain.title)
        assertEquals("Acme", domain.company)
        assertEquals("https://acme.io/job", domain.url)
        assertEquals("Paris", domain.location)
        assertEquals("LinkedIn", domain.source)
        assertEquals("50-60", domain.salaryRange)
        assertEquals(LocalDate(2026, 9, 5), domain.appliedDate)
        assertEquals(LocalDate(2026, 9, 12), domain.interviewDate)
        assertEquals(LocalDate(2026, 9, 19), domain.resultDate)
        assertEquals(ApplicationStatus.INTERVIEW, domain.status)
        assertEquals("à relancer", domain.notes)
    }

    @Test
    fun toDomain_entityWithoutOptionalFields_keepsThemNull() {
        val domain = jobOfferEntity().toDomain()

        assertNull(domain.url)
        assertNull(domain.location)
        assertNull(domain.source)
        assertNull(domain.salaryRange)
        assertNull(domain.notes)
    }

    @Test
    fun toDomain_nullInterviewAndResultDates_staysNull() {
        val domain = jobOfferEntity(interviewDateEpochDays = null, resultDateEpochDays = null).toDomain()

        assertNull(domain.interviewDate)
        assertNull(domain.resultDate)
    }

    @Test
    fun toDomain_onlyInterviewDateSet_leavesResultDateNull() {
        val domain = jobOfferEntity(interviewDateEpochDays = 20_708, resultDateEpochDays = null).toDomain()

        assertEquals(LocalDate(2026, 9, 12), domain.interviewDate)
        assertNull(domain.resultDate)
    }

    @Test
    fun toDomain_epochDayZero_isJanuaryFirst1970() {
        assertEquals(LocalDate(1970, 1, 1), jobOfferEntity(appliedDateEpochDays = 0).toDomain().appliedDate)
    }

    @Test
    fun toDomain_negativeEpochDay_isDateBefore1970() {
        assertEquals(LocalDate(1969, 12, 31), jobOfferEntity(appliedDateEpochDays = -1).toDomain().appliedDate)
    }

    @Test
    fun toDomain_leapDayEpoch_isFebruary29() {
        assertEquals(LocalDate(2028, 2, 29), jobOfferEntity(appliedDateEpochDays = 21_243).toDomain().appliedDate)
    }

    @Test
    fun toDomain_everyStatus_isPreserved() {
        ApplicationStatus.entries.forEach { status ->
            assertEquals(status, jobOfferEntity(status = status).toDomain().status)
        }
    }

    @Test
    fun toDomain_blankButNonNullOptionalStrings_areNotConvertedToNull() {
        val domain = jobOfferEntity(location = "", notes = " ").toDomain()

        assertEquals("", domain.location)
        assertEquals(" ", domain.notes)
    }

    // ---------- toEntity ----------

    @Test
    fun toEntity_fullOffer_mapsEveryField() {
        val offer = jobOffer(
            id = 5, title = "Dev", company = "Acme", url = "u", location = "Lyon", source = "s",
            salaryRange = "40-45", appliedDate = LocalDate(2026, 9, 5), interviewDate = LocalDate(2026, 9, 12),
            resultDate = LocalDate(2026, 9, 19), status = ApplicationStatus.ACCEPTED, notes = "n",
        )

        val entity = offer.toEntity(existingCreatedAt = 4_242)

        assertEquals(5L, entity.id)
        assertEquals("Dev", entity.title)
        assertEquals("Acme", entity.company)
        assertEquals("u", entity.url)
        assertEquals("Lyon", entity.location)
        assertEquals("s", entity.source)
        assertEquals("40-45", entity.salaryRange)
        assertEquals(20_701L, entity.appliedDateEpochDays)
        assertEquals(20_708L, entity.interviewDateEpochDays)
        assertEquals(20_715L, entity.resultDateEpochDays)
        assertEquals(ApplicationStatus.ACCEPTED, entity.status)
        assertEquals("n", entity.notes)
    }

    @Test
    fun toEntity_offerWithoutOptionalFields_keepsThemNull() {
        val entity = jobOffer().toEntity()

        assertNull(entity.url)
        assertNull(entity.location)
        assertNull(entity.source)
        assertNull(entity.salaryRange)
        assertNull(entity.interviewDateEpochDays)
        assertNull(entity.resultDateEpochDays)
        assertNull(entity.notes)
    }

    @Test
    fun toEntity_withExistingCreatedAt_preservesIt() {
        assertEquals(1_234L, jobOffer().toEntity(existingCreatedAt = 1_234).createdAtEpochMillis)
    }

    @Test
    fun toEntity_withExistingCreatedAtZero_preservesZeroInsteadOfUsingNow() {
        // 0 est une valeur légitime : elle ne doit pas être traitée comme « absente ».
        assertEquals(0L, jobOffer().toEntity(existingCreatedAt = 0).createdAtEpochMillis)
    }

    @Test
    fun toEntity_withoutExistingCreatedAt_usesCurrentTime() {
        val before = Clock.System.now().toEpochMilliseconds()

        val createdAt = jobOffer().toEntity().createdAtEpochMillis

        val after = Clock.System.now().toEpochMilliseconds()
        assertTrue(createdAt in before..after, "createdAt=$createdAt attendu entre $before et $after")
    }

    @Test
    fun toEntity_epochDayConversion_handlesDatesBeforeAndAfter1970() {
        assertEquals(-1L, jobOffer(appliedDate = LocalDate(1969, 12, 31)).toEntity().appliedDateEpochDays)
        assertEquals(0L, jobOffer(appliedDate = LocalDate(1970, 1, 1)).toEntity().appliedDateEpochDays)
        assertEquals(21_243L, jobOffer(appliedDate = LocalDate(2028, 2, 29)).toEntity().appliedDateEpochDays)
    }

    @Test
    fun toEntity_everyStatus_isPreserved() {
        ApplicationStatus.entries.forEach { status ->
            assertEquals(status, jobOffer(status = status).toEntity().status)
        }
    }

    @Test
    fun toEntity_newOfferWithZeroId_keepsZeroSoRoomCanGenerateOne() {
        assertEquals(0L, jobOffer(id = 0).toEntity().id)
    }

    // ---------- aller-retour ----------

    @Test
    fun roundTrip_domainToEntityToDomain_isLossless() {
        val offer = jobOffer(
            id = 9, url = "u", location = "Nantes", source = "s", salaryRange = "1-2",
            interviewDate = LocalDate(2026, 10, 1), resultDate = LocalDate(2026, 10, 15),
            status = ApplicationStatus.REJECTED, notes = "n",
        )

        assertEquals(offer, offer.toEntity().toDomain())
    }

    @Test
    fun roundTrip_entityToDomainToEntity_isLosslessWhenCreatedAtIsPassedBack() {
        val entity = jobOfferEntity(
            id = 3, location = "Lille", interviewDateEpochDays = 20_708, status = ApplicationStatus.PENDING,
            createdAtEpochMillis = 777,
        )

        assertEquals(entity, entity.toDomain().toEntity(existingCreatedAt = entity.createdAtEpochMillis))
    }
}
