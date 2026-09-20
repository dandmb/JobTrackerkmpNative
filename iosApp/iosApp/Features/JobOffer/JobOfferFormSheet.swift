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
        let parts = (existingOffer?.salaryRange ?? "").split(separator: "-")
        _salaryMin = State(initialValue: parts.first.map { $0.filter(\.isNumber) } ?? "")
        _salaryMax = State(initialValue: parts.count > 1 ? parts[1].filter(\.isNumber) : "")
        _notes = State(initialValue: existingOffer?.notes ?? "")
        _appliedDate = State(initialValue: existingOffer?.appliedDate.toDate() ?? Date())
        _interviewDate = State(initialValue: existingOffer?.interviewDate?.toDate())
        _resultDate = State(initialValue: existingOffer?.resultDate?.toDate())
    }

    private var isEditing: Bool { existingOffer != nil }

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

                Section("Salaire") {
                    HStack {
                        TextField("Min (k€)", text: $salaryMin)
                            .keyboardType(.numberPad)
                        TextField("Max (k€)", text: $salaryMax)
                            .keyboardType(.numberPad)
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
                        .disabled(title.trimmingCharacters(in: .whitespaces).isEmpty ||
                                  company.trimmingCharacters(in: .whitespaces).isEmpty)
                }
            }
        }
    }

    private func save() {
        let salaryRange: String? = {
            if !salaryMin.isEmpty && !salaryMax.isEmpty { return "\(salaryMin)k - \(salaryMax)k" }
            if !salaryMin.isEmpty { return "\(salaryMin)k+" }
            return nil
        }()

        let offer = JobOffer(
            id: existingOffer?.id ?? 0,
            title: title,
            company: company,
            url: url.isEmpty ? nil : url,
            location: location.isEmpty ? nil : location,
            source: source.isEmpty ? nil : source,
            salaryRange: salaryRange,
            appliedDate: appliedDate.toKotlinLocalDate(),
            interviewDate: interviewDate?.toKotlinLocalDate(),
            resultDate: resultDate?.toKotlinLocalDate(),
            status: existingOffer?.status ?? .applied,
            notes: notes.isEmpty ? nil : notes
        )

        if isEditing {
            viewModel.onUpdateOffer(offer: offer)
        } else {
            viewModel.onAddOffer(offer: offer)
        }
        onDone()
    }
}
