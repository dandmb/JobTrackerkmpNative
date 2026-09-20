import Foundation
import SharedLogic
@testable import JobTracker

/// Fabrique un `JobOffer` Kotlin valide ; on ne surcharge que ce que le test veut observer.
func makeOffer(
    id: Int64 = 0,
    title: String = "Développeur Kotlin",
    company: String = "Acme",
    location: String? = nil,
    salaryRange: String? = nil,
    applied: (Int32, Int32, Int32) = (2026, 9, 5),
    interview: (Int32, Int32, Int32)? = nil,
    result: (Int32, Int32, Int32)? = nil,
    status: ApplicationStatus = .applied
) -> JobOffer {
    func date(_ t: (Int32, Int32, Int32)) -> Kotlinx_datetimeLocalDate {
        Kotlinx_datetimeLocalDate(year: t.0, monthNumber: t.1, dayOfMonth: t.2)
    }
    return JobOffer(
        id: id, title: title, company: company, url: nil, location: location, source: nil,
        salaryRange: salaryRange, appliedDate: date(applied),
        interviewDate: interview.map(date), resultDate: result.map(date),
        status: status, notes: nil
    )
}
