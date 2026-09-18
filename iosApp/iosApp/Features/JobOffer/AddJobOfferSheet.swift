//
//  AddJobOfferSheet.swift
//  iosApp
//
//  Created by DAN BIZWA on 18/09/2026.
//

// iosApp/iosApp/Features/JobOffer/AddJobOfferSheet.swift
import SwiftUI
import SharedLogic

struct AddJobOfferSheet: View {
    let viewModel: JobOfferListViewModel
    @Environment(\.dismiss) private var dismiss

    @State private var title: String = ""
    @State private var company: String = ""
    @State private var url: String = ""

    var body: some View {
        NavigationStack {
            Form {
                Section("Détails de l'offre") {
                    TextField("Titre du poste", text: $title)
                    TextField("Entreprise", text: $company)
                    TextField("Lien de l'annonce (optionnel)", text: $url)
                        .keyboardType(.URL)
                        .autocapitalization(.none)
                }
            }
            .navigationTitle("Nouvelle candidature")
            .toolbar {
                ToolbarItem(placement: .cancellationAction) {
                    Button("Annuler") { dismiss() }
                }
                ToolbarItem(placement: .confirmationAction) {
                    Button("Ajouter") {
                        viewModel.onAddOffer(
                            title: title,
                            company: company,
                            url: url.isEmpty ? nil : url
                        )
                        dismiss()
                    }
                    .disabled(title.trimmingCharacters(in: .whitespaces).isEmpty ||
                              company.trimmingCharacters(in: .whitespaces).isEmpty)
                }
            }
        }
    }
}
