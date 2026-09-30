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
        resultDate: Date?,
        cvAttachmentId: Int64?,
        coverLetterAttachmentId: Int64?
    ) -> JobOffer {
        let draft = makeDraft(
            title: title, company: company, url: url, location: location, source: source, salaryMin: salaryMin,
            salaryMax: salaryMax, notes: notes, appliedDate: appliedDate, interviewDate: interviewDate, resultDate: resultDate,
            cvAttachmentId: cvAttachmentId, coverLetterAttachmentId: coverLetterAttachmentId
        )
        return JobOfferFormLogic.shared.toJobOffer(draft: draft, existing: existing)
    }

    static func hasUnsavedChanges(
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
        resultDate: Date?,
        cvAttachmentId: Int64?,
        coverLetterAttachmentId: Int64?,
        today: Date = Date()
    ) -> Bool {
        let draft = makeDraft(
            title: title, company: company, url: url, location: location, source: source, salaryMin: salaryMin,
            salaryMax: salaryMax, notes: notes, appliedDate: appliedDate, interviewDate: interviewDate, resultDate: resultDate,
            cvAttachmentId: cvAttachmentId, coverLetterAttachmentId: coverLetterAttachmentId
        )
        return JobOfferFormLogic.shared.hasUnsavedChanges(draft: draft, existing: existing, today: today.toKotlinLocalDate())
    }

    private static func makeDraft(
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
        resultDate: Date?,
        cvAttachmentId: Int64?,
        coverLetterAttachmentId: Int64?
    ) -> JobOfferFormDraft {
        JobOfferFormDraft(
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
            notes: notes,
            cvAttachmentId: cvAttachmentId.map { KotlinLong(value: $0) },
            coverLetterAttachmentId: coverLetterAttachmentId.map { KotlinLong(value: $0) }
        )
    }
}
