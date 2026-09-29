
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
            notes: notes,
            cvAttachmentId: cvAttachmentId.map { KotlinLong(value: $0) },
            coverLetterAttachmentId: coverLetterAttachmentId.map { KotlinLong(value: $0) }
        )
        return JobOfferFormLogic.shared.toJobOffer(draft: draft, existing: existing)
    }
}
