import XCTest
import SharedLogic
@testable import JobLog

/// La logique de formulaire est testée UNE fois dans sharedLogic (`JobOfferFormLogicTest`, JVM + natif).
/// Ici on vérifie seulement le pont côté iOS : que Swift appelle bien la fonction partagée (types, nil, Date → LocalDate)
/// et que l'écran ne réimplémente aucune règle.
final class JobOfferFormBridgeTests: XCTestCase {

    private let calendar = Calendar(identifier: .gregorian)

    private func date(_ y: Int, _ m: Int, _ d: Int) -> Date {
        calendar.date(from: DateComponents(year: y, month: m, day: d))!
    }

    private func build(
        existing: JobOffer? = nil, title: String = "Dev", company: String = "Acme", url: String = "", location: String = "",
        source: String = "", salaryMin: String = "", salaryMax: String = "", notes: String = "",
        applied: Date? = nil, interview: Date? = nil, result: Date? = nil
    ) -> JobOffer {
        JobOfferFormBridge.buildOffer(
            existing: existing, title: title, company: company, url: url, location: location, source: source,
            salaryMin: salaryMin, salaryMax: salaryMax, notes: notes,
            appliedDate: applied ?? date(2026, 9, 5), interviewDate: interview, resultDate: result
        )
    }

    // MARK: appel de la logique partagée depuis Swift

    func test_sharedLogic_parseSalaryFields_isCallableFromSwiftAndReturnsSharedResult() {
        let fields = JobOfferFormLogic.shared.parseSalaryFields(salaryRange: "55k - 70k")

        XCTAssertEqual(fields.min, "55")
        XCTAssertEqual(fields.max, "70")
    }

    func test_sharedLogic_parseSalaryFields_nilRange_returnsEmptyFields() {
        let fields = JobOfferFormLogic.shared.parseSalaryFields(salaryRange: nil)

        XCTAssertEqual(fields.min, "")
        XCTAssertEqual(fields.max, "")
    }

    func test_sharedLogic_nextSalaryInput_appliesTheSameFilterAsAndroid() {
        XCTAssertEqual(JobOfferFormLogic.shared.nextSalaryInput(previous: "1", input: "1a2"), "12")
        XCTAssertEqual(JobOfferFormLogic.shared.nextSalaryInput(previous: "1234", input: "12345"), "1234")
    }

    func test_sharedLogic_validate_refusesANewlineOnlyTitleLikeAndroid() {
        XCTAssertFalse(JobOfferFormLogic.shared.validate(title: "\n", company: "Acme", salaryMin: "", salaryMax: "", language: AppLanguage.fr).isValid)
        XCTAssertTrue(JobOfferFormLogic.shared.validate(title: "Dev", company: "Acme", salaryMin: "", salaryMax: "", language: AppLanguage.fr).isValid)
    }

    func test_sharedLogic_validate_blocksMaxWithoutMinAndExposesTheMessageToSwift() {
        let result = JobOfferFormLogic.shared.validate(title: "Dev", company: "Acme", salaryMin: "", salaryMax: "70", language: AppLanguage.fr)

        XCTAssertFalse(result.isValid)
        XCTAssertEqual(result.errorMessage, "Renseigne aussi le salaire minimum, ou laisse les deux champs vides.")
        XCTAssertEqual(JobOfferFormLogic.shared.validate(title: "Dev", company: "Acme", salaryMin: "", salaryMax: "70", language: AppLanguage.en).errorMessage,
                       "Please also enter the minimum salary, or leave both fields empty.")
    }

    func test_sharedLogic_validate_isUnblockedWhenTheMinIsFilledOrTheMaxCleared() {
        XCTAssertTrue(JobOfferFormLogic.shared.validate(title: "Dev", company: "Acme", salaryMin: "55", salaryMax: "70", language: AppLanguage.fr).isValid)
        XCTAssertTrue(JobOfferFormLogic.shared.validate(title: "Dev", company: "Acme", salaryMin: "", salaryMax: "", language: AppLanguage.fr).isValid)
    }

    func test_sharedLogic_validate_missingRequiredFieldHasNoMessage() {
        let result = JobOfferFormLogic.shared.validate(title: "", company: "Acme", salaryMin: "", salaryMax: "", language: AppLanguage.fr)

        XCTAssertFalse(result.isValid)
        XCTAssertNil(result.errorMessage)
    }

    // MARK: adaptateur buildOffer (conversion des dates + délégation)

    func test_buildOffer_dates_areConvertedToKotlinLocalDates() {
        let offer = build(applied: date(2026, 9, 5), interview: date(2026, 9, 12), result: date(2026, 9, 19))

        XCTAssertEqual(offer.appliedDate.dayOfMonth, 5)
        XCTAssertEqual(offer.interviewDate?.dayOfMonth, 12)
        XCTAssertEqual(offer.resultDate?.monthNumber, 9)
    }

    func test_buildOffer_noOptionalDates_keepsThemNil() {
        let offer = build(interview: nil, result: nil)

        XCTAssertNil(offer.interviewDate)
        XCTAssertNil(offer.resultDate)
    }

    func test_buildOffer_blankOptionalFields_becomeNilBecauseTheSharedRuleApplies() {
        let offer = build(url: "  ", location: "  ", source: "\t", notes: "   ")

        XCTAssertNil(offer.url)
        XCTAssertNil(offer.location)
        XCTAssertNil(offer.source)
        XCTAssertNil(offer.notes)
    }

    func test_buildOffer_salaryIsComposedByTheSharedLogic_maxOnlyIsNeverWritten() {
        XCTAssertEqual(build(salaryMin: "55", salaryMax: "70").salaryRange, "55k - 70k")
        XCTAssertEqual(build(salaryMin: "55").salaryRange, "55k+")
        XCTAssertNil(build(salaryMax: "70").salaryRange, "max seul : refusé en amont par validate(), jamais écrit")
        XCTAssertNil(build().salaryRange)
    }

    func test_buildOffer_newOffer_hasIdZeroAndAppliedStatus() {
        let offer = build()

        XCTAssertEqual(offer.id, 0)
        XCTAssertEqual(offer.status, .applied)
    }

    func test_buildOffer_editedOffer_keepsItsIdAndStatus() {
        let offer = build(existing: makeOffer(id: 7, status: .rejected))

        XCTAssertEqual(offer.id, 7)
        XCTAssertEqual(offer.status, .rejected)
    }

    func test_editThenSave_existingOfferLoadedIntoTheFormAndSavedBack_isUnchanged() {
        let existing = makeOffer(
            id: 3, title: "Dev", company: "Acme", location: "Lyon", salaryRange: "55k - 70k",
            applied: (2026, 9, 5), interview: (2026, 9, 12), result: nil, status: .interview
        )
        let salary = JobOfferFormLogic.shared.parseSalaryFields(salaryRange: existing.salaryRange)

        let saved = build(
            existing: existing, title: existing.title, company: existing.company, location: existing.location ?? "",
            salaryMin: salary.min, salaryMax: salary.max,
            applied: existing.appliedDate.toDate(), interview: existing.interviewDate?.toDate(), result: existing.resultDate?.toDate()
        )

        XCTAssertEqual(saved.id, existing.id)
        XCTAssertEqual(saved.status, existing.status)
        XCTAssertEqual(saved.location, existing.location)
        XCTAssertEqual(saved.salaryRange, existing.salaryRange)
        XCTAssertEqual(saved.appliedDate, existing.appliedDate)
        XCTAssertEqual(saved.interviewDate, existing.interviewDate)
        XCTAssertNil(saved.resultDate)
    }

    // MARK: garde-fou : l'écran ne réimplémente aucune règle

    private var sheetSource: String {
        let projectDir = URL(fileURLWithPath: #filePath).deletingLastPathComponent().deletingLastPathComponent()
        let file = projectDir.appendingPathComponent("iosApp/Features/JobOffer/JobOfferFormSheet.swift")
        return (try? String(contentsOf: file, encoding: .utf8)) ?? ""
    }

    func test_formSheet_sourceIsReadable() {
        XCTAssertFalse(sheetSource.isEmpty, "source de JobOfferFormSheet.swift introuvable depuis le test (chemin #filePath)")
    }

    func test_formSheet_callsTheSharedFormLogicForEveryRule() {
        for call in [
            "JobOfferFormLogic.shared.parseSalaryFields(",
            "JobOfferFormLogic.shared.nextSalaryInput(",
            "JobOfferFormLogic.shared.validate(",
            "validation.isValid",
            "validation.errorMessage",
            "JobOfferFormBridge.buildOffer(",
        ] {
            XCTAssertTrue(sheetSource.contains(call), "JobOfferFormSheet doit appeler \(call)")
        }
    }

    func test_formSheet_doesNotReimplementParsingOrValidationRules() {
        for forbidden in [
            "split(separator: \"-\")",
            "isEmpty ? nil",
            "trimmingCharacters(in: .whitespaces)",
            "\"k - \"",
            "filter(\\.isNumber)",
            "jusqu'à",
            "isFormSavable",
        ] {
            XCTAssertFalse(sheetSource.contains(forbidden), "JobOfferFormSheet ne doit plus contenir la règle « \(forbidden) » (déplacée dans sharedLogic)")
        }
    }
}
