//
//  JobOfferFormSheet.swift
//  iosApp
//
//  Created by DAN BIZWA on 19/09/2026.
//

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
    }

    private var isEditing: Bool { existingOffer != nil }

    // Validation partagée (sharedLogic) : Enregistrer n'est actif que si `isValid` ; `errorMessage` (salaire max sans min) est affiché
    private var validation: FormValidation {
        JobOfferFormLogic.shared.validate(title: title, company: company, salaryMin: salaryMin, salaryMax: salaryMax)
    }

    var body: some View {
        NavigationStack {
            Form {
                Section("Poste") {
                    TextField("Titre du poste", text: $title)
                    TextField("Entreprise", text: $company)
                    TextField("Localisation (ville, remote...)", text: $location)
                    TextField("Source (LinkedIn, cooptation...)", text: $source)
                    TextField("Lien de l'annonce", text: $url)
                        .keyboardType(.URL)
                        .autocapitalization(.none)
                }

                Section {
                    HStack {
                        // Même filtre de saisie que sur Android (4 chiffres, non-chiffres retirés) : règle partagée
                        TextField("Min (k€)", text: $salaryMin)
                            .keyboardType(.numberPad)
                            .onChange(of: salaryMin) { old, new in
                                salaryMin = JobOfferFormLogic.shared.nextSalaryInput(previous: old, input: new)
                            }
                        TextField("Max (k€)", text: $salaryMax)
                            .keyboardType(.numberPad)
                            .onChange(of: salaryMax) { old, new in
                                salaryMax = JobOfferFormLogic.shared.nextSalaryInput(previous: old, input: new)
                            }
                    }
                } header: {
                    Text("Salaire")
                } footer: {
                    // Message de validation en pied de section (convention SwiftUI/HIG), lu par VoiceOver avec la section
                    if let message = validation.errorMessage {
                        Text(message).foregroundStyle(.red)
                    }
                }

                Section("Dates") {
                    DatePicker("Date de candidature", selection: $appliedDate, displayedComponents: .date)

                    Toggle("Entretien programmé", isOn: Binding(
                        get: { interviewDate != nil },
                        set: { interviewDate = $0 ? Date() : nil }
                    ))
                    if let date = interviewDate {
                        DatePicker("Date d'entretien",
                                   selection: Binding(get: { date }, set: { interviewDate = $0 }),
                                   displayedComponents: .date)
                    }

                    Toggle("Résultat reçu", isOn: Binding(
                        get: { resultDate != nil },
                        set: { resultDate = $0 ? Date() : nil }
                    ))
                    if let date = resultDate {
                        DatePicker("Date de résultat",
                                   selection: Binding(get: { date }, set: { resultDate = $0 }),
                                   displayedComponents: .date)
                    }
                }

                Section("Notes") {
                    TextEditor(text: $notes).frame(minHeight: 80)
                }
            }
            .navigationTitle(isEditing ? "Modifier" : "Nouvelle candidature")
            .toolbar {
                ToolbarItem(placement: .cancellationAction) {
                    Button("Annuler") { onDone() }
                }
                ToolbarItem(placement: .confirmationAction) {
                    Button("Enregistrer") { save() }
                        .disabled(!validation.isValid)
                }
            }
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
            resultDate: resultDate
        )

        if isEditing {
            viewModel.onUpdateOffer(offer: offer)
        } else {
            viewModel.onAddOffer(offer: offer)
        }
        onDone()
    }
}
