//
//  JobOfferListView.swift
//  iosApp
//
//  Created by DAN BIZWA on 18/09/2026.
//

import Foundation
import SwiftUI
import SharedLogic
import KMPNativeCoroutinesAsync

@MainActor
class JobOfferListObservable: ObservableObject {
    let viewModelRef: JobOfferListViewModel
    private let viewModel: JobOfferListViewModel

    @Published var state: JobOfferListState
    private var task: Task<Void, Never>?

    init(viewModel: JobOfferListViewModel) {
        self.viewModelRef = viewModel
        self.viewModel = viewModel
        self.state = viewModel.state
        observe()
    }
    private func observe() {
        task = Task {
            do {
                let sequence = asyncSequence(for: viewModel.stateFlow)
                for try await newState in sequence {
                    self.state = newState
                }
            } catch {
                print("Erreur d'observation du state: \(error)")
            }
        }
    }

    deinit {
        task?.cancel()
    }
}



extension JobOffer: @retroactive Identifiable {}

struct JobOfferListView: View {
    @StateObject private var observable: JobOfferListObservable
    @State private var showingAddSheet = false
    @State private var offerBeingEdited: JobOffer?
    @State private var offerPendingDeletion: JobOffer?
    @State private var searchQuery = ""
    @State private var sortOption: SortOption = .dateDesc

    init(viewModel: JobOfferListViewModel) {
        _observable = StateObject(wrappedValue: JobOfferListObservable(viewModel: viewModel))
    }

    private var visibleOffers: [JobOffer] {
        observable.state.offers.searchedAndSorted(query: searchQuery, sortOption: sortOption)
    }

    var body: some View {
            NavigationStack {
                ZStack(alignment: .bottomTrailing) {
                    Group {
                        if observable.state.isLoading {
                            ProgressView()
                        } else if observable.state.offers.isEmpty {
                            // Miroir de EmptyOffersMessage (JobOfferListScreen.kt)
                            VStack(spacing: 8) {
                                Text("Aucune candidature")
                                    .appTextStyle(.titleMedium)
                                    .foregroundStyle(Color.onAppBackground)
                                Text("Ajoute ta première candidature avec le bouton +")
                                    .appTextStyle(.bodyMedium)
                                    .foregroundStyle(Color.onSurfaceVariant)
                                    .multilineTextAlignment(.center)
                            }
                            .padding(24)
                        } else {
                            List {
                                Section {
                                    JobOfferStatsCard(offers: observable.state.offers)
                                }
                                // Marge extérieure de la carte stats : 16 sur les côtés (comme Android : Column.padding(16.dp))
                                // et 8 + 4 (marge haute des cartes suivantes) = 12 d'écart (Android : spacedBy(12.dp)).
                                // Avant : EdgeInsets() à zéro → carte collée aux bords de l'écran et à la 1re candidature.
                                .listRowInsets(EdgeInsets(top: 8, leading: 16, bottom: 8, trailing: 16))
                                .listRowSeparator(.hidden)
                                .listRowBackground(Color.clear)

                                if visibleOffers.isEmpty {
                                    Text("Aucun résultat pour « \(searchQuery) »")
                                        .appTextStyle(.bodyLarge)
                                        .foregroundStyle(Color.onSurfaceVariant)
                                        .listRowSeparator(.hidden)
                                        .listRowBackground(Color.clear)
                                } else {
                                    ForEach(visibleOffers) { offer in
                                        JobOfferCard(
                                            offer: offer,
                                            onStatusChanged: { newStatus in
                                                observable.viewModelRef.onStatusChanged(offer: offer, newStatus: newStatus)
                                            },
                                            onEditTap: { offerBeingEdited = offer }
                                        )
                                        .listRowInsets(EdgeInsets(top: 4, leading: 16, bottom: 4, trailing: 16))
                                        .listRowSeparator(.hidden)
                                        .listRowBackground(Color.clear)
                                        .swipeActions(edge: .trailing) {
                                            Button(role: .destructive) {
                                                offerPendingDeletion = offer   // confirmation avant suppression (voir confirmationDialog)
                                            } label: {
                                                Label("Supprimer", systemImage: "trash")
                                            }
                                        }
                                    }
                                }
                            }
                            .listStyle(.plain)
                            .scrollContentBackground(.hidden)   // laisse voir le fond appBackground (Scaffold Android)
                            // Marge basse : le FAB (56 pt + 20 pt de marge) ne doit pas masquer la dernière carte
                            .contentMargins(.bottom, 88, for: .scrollContent)
                            // Force la recherche SOUS le titre, comportement classique et prévisible
                            .searchable(
                                text: $searchQuery,
                                placement: .navigationBarDrawer(displayMode: .always),
                                prompt: "Rechercher un poste ou une entreprise"
                            )
                        }
                    }
                    .frame(maxWidth: .infinity, maxHeight: .infinity)   // l'état vide doit remplir l'écran (le FAB reste en bas à droite)

                    Button(action: { showingAddSheet = true }) {
                        // FAB : secondary / onSecondary (Android : Icon Add)
                        Image(systemName: "plus")
                            .font(.system(size: 20, weight: .semibold))
                            .foregroundStyle(Color.onCoralSecondary)
                            .frame(width: 56, height: 56)
                            .background(Color.coralSecondary)
                            .clipShape(Circle())
                            .shadow(radius: 4)
                    }
                    .padding(20)
                    .accessibilityLabel("Ajouter une candidature")
                }
                .background(Color.appBackground.ignoresSafeArea())   // Scaffold : colorScheme.background
                .navigationTitle("Candidatures")
                .navigationBarTitleDisplayMode(.large)
                .toolbar {
                    ToolbarItem(placement: .primaryAction) {
                        Menu {
                            ForEach(SortOption.allCases) { option in
                                Button(option.rawValue) { sortOption = option }
                            }
                        } label: {
                            Image(systemName: "arrow.up.arrow.down")
                        }
                    }
                }
            }
            .tint(Color.tealPrimary)   // remplace toolbarBackground/toolbarColorScheme
            .sheet(isPresented: $showingAddSheet) {
                JobOfferFormSheet(existingOffer: nil, viewModel: observable.viewModelRef) {
                    showingAddSheet = false
                }
            }
            .confirmationDialog(
                "Supprimer cette candidature ?",
                isPresented: Binding(
                    get: { offerPendingDeletion != nil },
                    set: { if !$0 { offerPendingDeletion = nil } }
                ),
                titleVisibility: .visible,
                presenting: offerPendingDeletion
            ) { offer in
                Button("Supprimer", role: .destructive) {
                    observable.viewModelRef.onDeleteOffer(offer: offer)
                }
                Button("Annuler", role: .cancel) {}
            } message: { offer in
                Text("« \(offer.title) » sera supprimée définitivement.")
            }
            .sheet(item: $offerBeingEdited) { offer in
                JobOfferFormSheet(existingOffer: offer, viewModel: observable.viewModelRef) {
                    offerBeingEdited = nil
                }
            }
        }
}
