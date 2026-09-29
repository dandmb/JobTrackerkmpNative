
import SwiftUI
import SharedLogic

struct JobOfferFormSheet: View {
    let existingOffer: JobOffer?
    let viewModel: JobOfferListViewModel
    let onDone: () -> Void

    @State private var title: String
    @State private var company: String
    @State private var url: String
    @State private var location: String
    @State private var source: String
    @State private var salaryMin: String
    @State private var salaryMax: String
    @State private var notes: String
    @State private var appliedDate: Date
    @State private var interviewDate: Date?
    @State private var resultDate: Date?
    @State private var cvAttachmentId: Int64?
    @State private var coverLetterAttachmentId: Int64?
    @StateObject private var attachments = AttachmentsObservable()
    @StateObject private var documents = DocumentsFormState()

    init(existingOffer: JobOffer?, viewModel: JobOfferListViewModel, onDone: @escaping () -> Void) {
        self.existingOffer = existingOffer
        self.viewModel = viewModel
        self.onDone = onDone
        _title = State(initialValue: existingOffer?.title ?? "")
        _company = State(initialValue: existingOffer?.company ?? "")
        _url = State(initialValue: existingOffer?.url ?? "")
        _location = State(initialValue: existingOffer?.location ?? "")
        _source = State(initialValue: existingOffer?.source ?? "")
        let salary = JobOfferFormLogic.shared.parseSalaryFields(salaryRange: existingOffer?.salaryRange)
        _salaryMin = State(initialValue: salary.min)
        _salaryMax = State(initialValue: salary.max)
        _notes = State(initialValue: existingOffer?.notes ?? "")
        _appliedDate = State(initialValue: existingOffer?.appliedDate.toDate() ?? Date())
        _interviewDate = State(initialValue: existingOffer?.interviewDate?.toDate())
        _resultDate = State(initialValue: existingOffer?.resultDate?.toDate())
        _cvAttachmentId = State(initialValue: existingOffer?.cvAttachmentId?.int64Value)
        _coverLetterAttachmentId = State(initialValue: existingOffer?.coverLetterAttachmentId?.int64Value)
    }

    private var isEditing: Bool { existingOffer != nil }

    private var validation: FormValidation {
        JobOfferFormLogic.shared.validate(title: title, company: company, salaryMin: salaryMin, salaryMax: salaryMax, language: AppLanguage.current)
    }

    var body: some View {
        NavigationStack {
            Form {
                Section("form_section_job") {
                    labeledField("form_field_title", text: $title)
                    labeledField("form_field_company", text: $company)
                    labeledField("form_field_location", text: $location)
                    labeledField("form_field_source", text: $source)
                    labeledField("form_field_url", text: $url)
                        .keyboardType(.URL)
                        .autocapitalization(.none)
                }

                Section {
                    HStack {
                        labeledField("form_salary_min_short", text: $salaryMin)
                            .keyboardType(.numberPad)
                            .onChange(of: salaryMin) { old, new in
                                salaryMin = JobOfferFormLogic.shared.nextSalaryInput(previous: old, input: new)
                            }
                        labeledField("form_salary_max_short", text: $salaryMax)
                            .keyboardType(.numberPad)
                            .onChange(of: salaryMax) { old, new in
                                salaryMax = JobOfferFormLogic.shared.nextSalaryInput(previous: old, input: new)
                            }
                    }
                } header: {
                    Text("form_section_salary")
                } footer: {
                    if let message = validation.errorMessage {
                        Text(message).foregroundStyle(.red)
                    }
                }

                Section("form_section_dates") {
                    DatePicker("form_date_applied", selection: $appliedDate, displayedComponents: .date)

                    Toggle("form_interview_toggle", isOn: Binding(
                        get: { interviewDate != nil },
                        set: { interviewDate = $0 ? Date() : nil }
                    ))
                    if let date = interviewDate {
                        DatePicker("form_date_interview_short",
                                   selection: Binding(get: { date }, set: { interviewDate = $0 }),
                                   displayedComponents: .date)
                    }

                    Toggle("form_result_toggle", isOn: Binding(
                        get: { resultDate != nil },
                        set: { resultDate = $0 ? Date() : nil }
                    ))
                    if let date = resultDate {
                        DatePicker("form_date_result_short",
                                   selection: Binding(get: { date }, set: { resultDate = $0 }),
                                   displayedComponents: .date)
                    }
                }

                DocumentsFormSection(
                    cvAttachmentId: $cvAttachmentId,
                    coverLetterAttachmentId: $coverLetterAttachmentId,
                    attachments: attachments,
                    state: documents
                )

                Section("form_field_notes") {
                    TextEditor(text: $notes).frame(minHeight: 80)
                }
            }
            .documentsFormPresentations(
                state: documents,
                attachments: attachments,
                cvAttachmentId: $cvAttachmentId,
                coverLetterAttachmentId: $coverLetterAttachmentId
            )
            .navigationTitle(isEditing ? "form_title_edit" : "form_title_new")
            .toolbar {
                ToolbarItem(placement: .cancellationAction) {
                    Button("cancel") { onDone() }
                }
                ToolbarItem(placement: .confirmationAction) {
                    Button("form_save") { save() }
                        .disabled(!validation.isValid)
                }
            }
        }
    }

    private func labeledField(_ label: LocalizedStringKey, text: Binding<String>) -> some View {
        VStack(alignment: .leading, spacing: 2) {
            Text(label)
                .appTextStyle(.labelLarge)
                .foregroundStyle(Color.onSurfaceVariant)
                .accessibilityHidden(true)
            TextField(text: text, prompt: Text(verbatim: "")) { Text(label) }
        }
    }

    private func save() {
        let offer = JobOfferFormBridge.buildOffer(
            existing: existingOffer,
            title: title,
            company: company,
            url: url,
            location: location,
            source: source,
            salaryMin: salaryMin,
            salaryMax: salaryMax,
            notes: notes,
            appliedDate: appliedDate,
            interviewDate: interviewDate,
            resultDate: resultDate,
            cvAttachmentId: cvAttachmentId,
            coverLetterAttachmentId: coverLetterAttachmentId
        )

        if isEditing {
            viewModel.onUpdateOffer(offer: offer)
        } else {
            viewModel.onAddOffer(offer: offer)
        }
        onDone()
    }
}
