package com.dmb.jobtracker.presentation.form

import com.dmb.jobtracker.domain.model.ApplicationStatus
import com.dmb.jobtracker.testutil.jobOffer
import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Logique de formulaire UNIQUE (Android + iOS), testée une seule fois sur les deux runtimes (JVM et Kotlin/Native).
 * Regroupe les cas des anciens `JobOfferFormLogicTest` (Android) et `JobOfferFormLogicTests` (iOS), plus les cas des
 * comportements de référence tranchés lors de l'unification (voir KDoc de [JobOfferFormLogic]).
 */
class JobOfferFormLogicTest {

    private val logic = JobOfferFormLogic

    private fun fields(min: String, max: String) = SalaryFields(min, max)

    // ---------- parseSalaryFields : entrées vides ----------

    @Test
    fun parseSalaryFields_nullRange_returnsEmptyFields() {
        assertEquals(fields("", ""), logic.parseSalaryFields(null))
    }

    @Test
    fun parseSalaryFields_emptyRange_returnsEmptyFields() {
        assertEquals(fields("", ""), logic.parseSalaryFields(""))
    }

    @Test
    fun parseSalaryFields_blankRange_returnsEmptyFields() {
        assertEquals(fields("", ""), logic.parseSalaryFields("   \t"))
    }

    // ---------- parseSalaryFields : les 2 formes écrites par le formulaire + le format « jusqu'à » hérité ----------

    @Test
    fun parseSalaryFields_closedRange_splitsMinAndMax() {
        assertEquals(fields("55", "70"), logic.parseSalaryFields("55k - 70k"))
    }

    @Test
    fun parseSalaryFields_openEndedRange_fillsOnlyMin() {
        assertEquals(fields("55", ""), logic.parseSalaryFields("55k+"))
    }

    @Test
    fun parseSalaryFields_legacyUpToRange_isStillReadAsMaxOnly() {
        // Le formulaire n'écrit plus « jusqu'à 70k » mais on le lit encore (build de test) : sans ce cas il serait lu comme
        // « min = 70 », un changement de sens silencieux.
        assertEquals(fields("", "70"), logic.parseSalaryFields("jusqu'à 70k"))
    }

    @Test
    fun parseSalaryFields_legacyUpToRangeWithTypographicApostropheAndCapital_isStillReadAsMaxOnly() {
        assertEquals(fields("", "70"), logic.parseSalaryFields("Jusqu’à 70k"))
    }

    // ---------- parseSalaryFields : texte libre hérité (grammaire explicite) ----------

    @Test
    fun parseSalaryFields_legacyFreeText_keepsDigitsOnEachSideOfTheDash() {
        assertEquals(fields("55000", "70000"), logic.parseSalaryFields("55000-70000 €"))
    }

    @Test
    fun parseSalaryFields_extraSpaces_areIgnored() {
        assertEquals(fields("55", "70"), logic.parseSalaryFields("   55k   -   70k  "))
    }

    @Test
    fun parseSalaryFields_singleValueWithoutDash_fillsOnlyMin() {
        assertEquals(fields("70", ""), logic.parseSalaryFields("70k"))
    }

    @Test
    fun parseSalaryFields_dashWithoutMin_fillsOnlyMaxByPosition() {
        // Décision : la position fait foi (à gauche du tiret = min, à droite = max) ; « -70k » = max seul, sur les 2 plateformes.
        assertEquals(fields("", "70"), logic.parseSalaryFields("-70k"))
    }

    @Test
    fun parseSalaryFields_dashWithoutMax_fillsOnlyMin() {
        assertEquals(fields("55", ""), logic.parseSalaryFields("55k - "))
    }

    @Test
    fun parseSalaryFields_onlyADash_returnsEmptyFields() {
        assertEquals(fields("", ""), logic.parseSalaryFields("-"))
    }

    @Test
    fun parseSalaryFields_nonNumericText_returnsEmptyFields() {
        assertEquals(fields("", ""), logic.parseSalaryFields("à négocier"))
    }

    @Test
    fun parseSalaryFields_severalDashes_isRejectedInsteadOfGuessing() {
        // Décision : « 55k - 70k - 80k » est ambigu → champs vides (avant : « 7080 » sur Android, « 70 » sur iOS).
        assertEquals(fields("", ""), logic.parseSalaryFields("55k - 70k - 80k"))
    }

    @Test
    fun parseSalaryFields_twoDashesEvenWithOneNumber_isRejected() {
        assertEquals(fields("", ""), logic.parseSalaryFields("--70k"))
    }

    @Test
    fun parseSalaryFields_decimalAmount_losesTheDecimalPoint() {
        // Limite connue et documentée : « 45.5k » devient 455 ; le formulaire n'écrit que des entiers, donc seuls des
        // textes saisis hors de l'app sont concernés.
        assertEquals(fields("455", "60"), logic.parseSalaryFields("45.5k - 60k"))
    }

    // ---------- composeSalaryRange ----------

    @Test
    fun composeSalaryRange_minAndMax_producesTheClosedRange() {
        assertEquals("55k - 70k", logic.composeSalaryRange("55", "70"))
    }

    @Test
    fun composeSalaryRange_onlyMin_producesTheOpenEndedRange() {
        assertEquals("55k+", logic.composeSalaryRange("55", ""))
    }

    @Test
    fun composeSalaryRange_onlyMax_producesNullNoUpToFormatAnymore() {
        // Décision 6 (révisée) : le format « jusqu'à » n'est plus produit. Le cas est refusé en amont par validate().
        assertNull(logic.composeSalaryRange("", "70"))
    }

    @Test
    fun composeSalaryRange_noField_producesNull() {
        assertNull(logic.composeSalaryRange("", ""))
    }

    @Test
    fun composeSalaryRange_blankMinAndFilledMax_producesNullNotAMalformedSegment() {
        assertNull(logic.composeSalaryRange("  ", "70"))
    }

    @Test
    fun composeSalaryRange_filledMinAndBlankMax_producesTheOpenEndedRange() {
        assertEquals("55k+", logic.composeSalaryRange("55", "  "))
    }

    @Test
    fun composeSalaryRange_bothBlank_producesNull() {
        assertNull(logic.composeSalaryRange(" ", "\t"))
    }

    @Test
    fun composeSalaryRange_nonDigitCharacters_areIgnored() {
        assertEquals("55k - 70k", logic.composeSalaryRange("5a5", "7-0"))
        assertNull(logic.composeSalaryRange("abc", "xyz"))
    }

    // ---------- aller-retour compose → parse (les 3 formes sont relues à l'identique) ----------

    @Test
    fun composeThenParse_closedRange_roundTripsTheFields() {
        assertEquals(fields("55", "70"), logic.parseSalaryFields(logic.composeSalaryRange("55", "70")))
    }

    @Test
    fun composeThenParse_openEndedRange_roundTripsTheFields() {
        assertEquals(fields("55", ""), logic.parseSalaryFields(logic.composeSalaryRange("55", "")))
    }

    @Test
    fun composeThenParse_maxOnly_isNotWrittenSoNothingIsReadBack() {
        assertEquals(fields("", ""), logic.parseSalaryFields(logic.composeSalaryRange("", "70")))
    }

    @Test
    fun parseThenCompose_theTwoWrittenForms_areStable() {
        listOf("55k - 70k", "55k+").forEach { stored ->
            val parsed = logic.parseSalaryFields(stored)

            assertEquals(stored, logic.composeSalaryRange(parsed.min, parsed.max), stored)
        }
    }

    // ---------- nextSalaryInput (filtre de saisie, identique sur les 2 plateformes) ----------

    @Test
    fun nextSalaryInput_digitsWithinTheLimit_areAccepted() {
        assertEquals("55", logic.nextSalaryInput(previous = "5", input = "55"))
        assertEquals("1234", logic.nextSalaryInput(previous = "123", input = "1234"))
    }

    @Test
    fun nextSalaryInput_nonDigitCharacters_areFilteredOut() {
        assertEquals("12", logic.nextSalaryInput(previous = "1", input = "1a2"))
    }

    @Test
    fun nextSalaryInput_onlyLetters_becomesEmpty() {
        assertEquals("", logic.nextSalaryInput(previous = "5", input = "abcd"))
    }

    @Test
    fun nextSalaryInput_exactlyFourCharacters_isAccepted() {
        assertEquals("9999", logic.nextSalaryInput(previous = "999", input = "9999"))
    }

    @Test
    fun nextSalaryInput_fiveCharacters_isIgnoredAndKeepsThePreviousValue() {
        assertEquals("1234", logic.nextSalaryInput(previous = "1234", input = "12345"))
    }

    @Test
    fun nextSalaryInput_pastedLongValue_isIgnoredInsteadOfTruncated() {
        assertEquals("55", logic.nextSalaryInput(previous = "55", input = "123456789"))
    }

    @Test
    fun nextSalaryInput_clearingTheField_returnsEmpty() {
        assertEquals("", logic.nextSalaryInput(previous = "55", input = ""))
    }

    @Test
    fun nextSalaryInput_appliedTwice_isIdempotent() {
        val once = logic.nextSalaryInput(previous = "1", input = "1a2")

        assertEquals(once, logic.nextSalaryInput(previous = "1", input = once))
    }

    @Test
    fun salaryInputMaxLength_isFour() {
        assertEquals(4, JobOfferFormLogic.SALARY_INPUT_MAX_LENGTH)
    }

    // ---------- validate : champs obligatoires (règle 2) ----------

    private fun validate(title: String = "Dev", company: String = "Acme", min: String = "", max: String = "") =
        logic.validate(title, company, min, max)

    @Test
    fun validate_titleAndCompanyFilled_isValid() {
        assertEquals(FormValidation.Valid, validate())
        assertTrue(validate().isValid)
    }

    @Test
    fun validate_blankTitle_isMissingRequiredFieldAndNotValid() {
        assertEquals(FormValidation.MissingRequiredField, validate(title = ""))
        assertEquals(FormValidation.MissingRequiredField, validate(title = "   "))
    }

    @Test
    fun validate_blankCompany_isMissingRequiredFieldAndNotValid() {
        assertEquals(FormValidation.MissingRequiredField, validate(company = ""))
        assertEquals(FormValidation.MissingRequiredField, validate(company = "\t"))
    }

    @Test
    fun validate_bothBlank_isMissingRequiredField() {
        assertEquals(FormValidation.MissingRequiredField, validate(title = "", company = ""))
    }

    @Test
    fun validate_newlineOnlyTitle_isRefusedLikeAnEmptyTitle() {
        assertFalse(validate(title = "\n").isValid)
        assertFalse(validate(company = " \r\n ").isValid)
    }

    @Test
    fun validate_textWithSurroundingSpaces_isValid() {
        assertTrue(validate(title = "  Dev  ", company = " Acme ").isValid)
    }

    @Test
    fun validate_missingRequiredField_hasNoMessageToShow() {
        // Comportement historique : titre/entreprise vides = bouton désactivé, sans message.
        assertNull(validate(title = "").errorMessage)
        assertFalse(validate(title = "").isValid)
    }

    // ---------- validate : salaire max sans min (règle 6, bloquante) ----------

    @Test
    fun validate_maxWithoutMin_isInvalidWithTheClearMessage() {
        val result = validate(max = "70")

        assertEquals(FormValidation.InvalidSalary("Renseigne aussi le salaire minimum, ou laisse les deux champs vides."), result)
        assertFalse(result.isValid)
        assertEquals("Renseigne aussi le salaire minimum, ou laisse les deux champs vides.", result.errorMessage)
    }

    @Test
    fun validate_blankMinWithMax_isInvalid() {
        assertFalse(validate(min = "   ", max = "70").isValid)
    }

    @Test
    fun validate_nonNumericMinWithMax_isInvalidBecauseTheMinIsEmptyOfDigits() {
        assertFalse(validate(min = "abc", max = "70").isValid)
    }

    @Test
    fun validate_maxWithoutMin_isUnblockedByFillingTheMin() {
        assertFalse(validate(max = "70").isValid)

        assertTrue(validate(min = "55", max = "70").isValid)
    }

    @Test
    fun validate_maxWithoutMin_isUnblockedByClearingTheMax() {
        assertFalse(validate(max = "70").isValid)

        assertTrue(validate(max = "").isValid)
    }

    @Test
    fun validate_minOnly_isValid() {
        assertTrue(validate(min = "55").isValid)
        assertNull(validate(min = "55").errorMessage)
    }

    @Test
    fun validate_bothSalaryFieldsEmpty_isValid() {
        assertTrue(validate(min = "", max = "").isValid)
    }

    @Test
    fun validate_bothSalaryFieldsFilled_isValid() {
        assertTrue(validate(min = "55", max = "70").isValid)
    }

    @Test
    fun validate_validForm_hasNoErrorMessage() {
        assertNull(validate().errorMessage)
    }

    @Test
    fun validate_maxWithoutMinAndBlankTitle_showsTheSalaryMessageFirst() {
        // Le message de salaire est le seul à afficher : il apparaît dès que le max est rempli, même titre encore vide.
        val result = validate(title = "", max = "70")

        assertEquals(FormValidation.InvalidSalary(JobOfferFormLogic.SALARY_MIN_REQUIRED_MESSAGE), result)
    }

    @Test
    fun validate_afterFixingTheSalary_fallsBackToTheRequiredFieldState() {
        assertEquals(FormValidation.MissingRequiredField, validate(title = "", min = "55", max = "70"))
    }

    @Test
    fun salaryMinRequiredMessage_isTheDocumentedFrenchSentence() {
        assertEquals("Renseigne aussi le salaire minimum, ou laisse les deux champs vides.", JobOfferFormLogic.SALARY_MIN_REQUIRED_MESSAGE)
    }

    @Test
    fun formValidation_valid_isValidWithoutMessage() {
        assertTrue(FormValidation.Valid.isValid)
        assertNull(FormValidation.Valid.errorMessage)
    }

    // ---------- initialAppliedDate ----------

    @Test
    fun initialAppliedDate_newOffer_isToday() {
        val today = LocalDate(2026, 9, 20)

        assertEquals(today, logic.initialAppliedDate(existing = null, today = today))
    }

    @Test
    fun initialAppliedDate_editedOffer_keepsItsOwnDate() {
        val existing = jobOffer(appliedDate = LocalDate(2026, 3, 1))

        assertEquals(LocalDate(2026, 3, 1), logic.initialAppliedDate(existing, today = LocalDate(2026, 9, 20)))
    }

    // ---------- toJobOffer ----------

    private fun draft(
        title: String = "Dev", company: String = "Acme", url: String = "", location: String = "", source: String = "",
        salaryMin: String = "", salaryMax: String = "", applied: LocalDate = LocalDate(2026, 9, 5),
        interview: LocalDate? = null, result: LocalDate? = null, notes: String = "",
    ) = JobOfferFormDraft(title, company, url, location, source, salaryMin, salaryMax, applied, interview, result, notes)

    @Test
    fun toJobOffer_newOffer_hasIdZeroAndAppliedStatus() {
        val result = logic.toJobOffer(draft(), existing = null)

        assertEquals(0L, result.id)
        assertEquals(ApplicationStatus.APPLIED, result.status)
    }

    @Test
    fun toJobOffer_editedOffer_keepsItsIdAndStatus() {
        val result = logic.toJobOffer(draft(), existing = jobOffer(id = 7, status = ApplicationStatus.REJECTED))

        assertEquals(7L, result.id)
        assertEquals(ApplicationStatus.REJECTED, result.status)
    }

    @Test
    fun toJobOffer_emptyOptionalTextFields_becomeNull() {
        val result = logic.toJobOffer(draft(url = "", location = "", source = "", notes = ""), null)

        assertNull(result.url)
        assertNull(result.location)
        assertNull(result.source)
        assertNull(result.notes)
    }

    @Test
    fun toJobOffer_blankOptionalTextFields_becomeNullOnBothPlatforms() {
        // Décision : un champ optionnel blanc n'est jamais stocké comme « "  " » (avant : conservé tel quel sur iOS).
        val result = logic.toJobOffer(draft(url = "  ", location = "  ", source = "\t", notes = "   "), null)

        assertNull(result.url)
        assertNull(result.location)
        assertNull(result.source)
        assertNull(result.notes)
    }

    @Test
    fun toJobOffer_filledOptionalTextFields_arePassedThroughUntrimmed() {
        val result = logic.toJobOffer(draft(url = "https://a.io", location = " Paris ", source = "LinkedIn", notes = "à relancer"), null)

        assertEquals("https://a.io", result.url)
        assertEquals(" Paris ", result.location)
        assertEquals("LinkedIn", result.source)
        assertEquals("à relancer", result.notes)
    }

    @Test
    fun toJobOffer_titleAndCompany_arePassedThroughWithoutTrimming() {
        val result = logic.toJobOffer(draft(title = "  Dev  ", company = " Acme "), null)

        assertEquals("  Dev  ", result.title)
        assertEquals(" Acme ", result.company)
    }

    @Test
    fun toJobOffer_salaryFields_areComposedIntoTheStoredRange() {
        assertEquals("55k - 70k", logic.toJobOffer(draft(salaryMin = "55", salaryMax = "70"), null).salaryRange)
        assertEquals("55k+", logic.toJobOffer(draft(salaryMin = "55"), null).salaryRange)
        assertNull(logic.toJobOffer(draft(salaryMax = "70"), null).salaryRange, "max seul : refusé en amont par validate(), jamais écrit")
        assertNull(logic.toJobOffer(draft(), null).salaryRange)
    }

    @Test
    fun toJobOffer_noOptionalDates_keepsThemNull() {
        val result = logic.toJobOffer(draft(interview = null, result = null), null)

        assertNull(result.interviewDate)
        assertNull(result.resultDate)
    }

    @Test
    fun toJobOffer_optionalDatesSet_arePassedThrough() {
        val result = logic.toJobOffer(draft(interview = LocalDate(2026, 9, 12), result = LocalDate(2026, 9, 19)), null)

        assertEquals(LocalDate(2026, 9, 12), result.interviewDate)
        assertEquals(LocalDate(2026, 9, 19), result.resultDate)
    }

    @Test
    fun toJobOffer_editedOfferWithNullOptionalDates_staysNullWhenUntouched() {
        val existing = jobOffer(interviewDate = null, resultDate = null)
        val untouched = draft(applied = existing.appliedDate, interview = existing.interviewDate, result = existing.resultDate)

        val result = logic.toJobOffer(untouched, existing)

        assertNull(result.interviewDate)
        assertNull(result.resultDate)
        assertEquals(existing.appliedDate, result.appliedDate)
    }

    @Test
    fun toJobOffer_optionalDateCleared_becomesNull() {
        val existing = jobOffer(interviewDate = LocalDate(2026, 9, 12))

        val result = logic.toJobOffer(draft(applied = existing.appliedDate, interview = null), existing)

        assertNull(result.interviewDate)
    }

    @Test
    fun editThenSave_existingOfferLoadedIntoTheFormAndSavedBack_isUnchanged() {
        val existing = jobOffer(
            id = 3, title = "Dev", company = "Acme", url = "u", location = "Lyon", source = "s",
            salaryRange = "55k - 70k", appliedDate = LocalDate(2026, 9, 5), interviewDate = LocalDate(2026, 9, 12),
            resultDate = null, status = ApplicationStatus.INTERVIEW, notes = "n",
        )
        val salary = logic.parseSalaryFields(existing.salaryRange)
        val loaded = JobOfferFormDraft(
            title = existing.title, company = existing.company, url = existing.url.orEmpty(),
            location = existing.location.orEmpty(), source = existing.source.orEmpty(),
            salaryMin = salary.min, salaryMax = salary.max,
            appliedDate = logic.initialAppliedDate(existing, LocalDate(2030, 1, 1)),
            interviewDate = existing.interviewDate, resultDate = existing.resultDate, notes = existing.notes.orEmpty(),
        )

        assertEquals(existing, logic.toJobOffer(loaded, existing))
    }

    @Test
    fun editThenSave_offerStoredWithLegacyUpToFormat_opensAsMaxOnlyAndMustBeFixedBeforeSaving() {
        // Donnée d'une build de test : rien n'est perdu ni réinterprété ; l'utilisateur voit le message et complète ou vide le max.
        val salary = logic.parseSalaryFields("jusqu'à 70k")

        val validation = logic.validate("Dev", "Acme", salary.min, salary.max)

        assertEquals(FormValidation.InvalidSalary(JobOfferFormLogic.SALARY_MIN_REQUIRED_MESSAGE), validation)
    }

    @Test
    fun editThenSave_offerWithOpenEndedSalary_keepsItsSalary() {
        val existing = jobOffer(id = 3, salaryRange = "55k+")
        val salary = logic.parseSalaryFields(existing.salaryRange)

        assertEquals("55k+", logic.toJobOffer(draft(salaryMin = salary.min, salaryMax = salary.max), existing).salaryRange)
    }
}
