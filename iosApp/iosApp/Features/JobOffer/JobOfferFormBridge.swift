
import Foundation
import SharedLogic

enum JobOfferFormBridge {

    static func buildOffer(
        existing: JobOffer?,
        title: String,
        company: String,
        url: String,
        location: String,
        source: String,
        salaryMin: String,
        salaryMax: String,
        notes: String,
        appliedDate: Date,
        interviewDate: Date?,
        resultDate: Date?
    ) -> JobOffer {
        let draft = JobOfferFormDraft(
            title: title,
            company: company,
            url: url,
            location: location,
            source: source,
            salaryMin: salaryMin,
            salaryMax: salaryMax,
            appliedDate: appliedDate.toKotlinLocalDate(),
            interviewDate: interviewDate?.toKotlinLocalDate(),
            resultDate: resultDate?.toKotlinLocalDate(),
            notes: notes
        )
        return JobOfferFormLogic.shared.toJobOffer(draft: draft, existing: existing)
    }
}
