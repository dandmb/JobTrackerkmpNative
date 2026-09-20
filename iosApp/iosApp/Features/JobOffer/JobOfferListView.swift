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
    @State private var searchQuery = ""
    @State private var sortOption: SortOption = .dateDesc

    init(viewModel: JobOfferListViewModel) {
        _observable = StateObject(wrappedValue: JobOfferListObservable(viewModel: viewModel))
    }

    private var visibleOffers: [JobOffer] {
        let filtered = observable.state.offers.filter {
            searchQuery.isEmpty ||
            $0.title.localizedCaseInsensitiveContains(searchQuery) ||
            $0.company.localizedCaseInsensitiveContains(searchQuery)
        }
        switch sortOption {
        case .dateDesc: return filtered.sorted { $0.appliedDate.toDate() > $1.appliedDate.toDate() }
        case .dateAsc: return filtered.sorted { $0.appliedDate.toDate() < $1.appliedDate.toDate() }
        case .alphaAsc: return filtered.sorted { $0.title.lowercased() < $1.title.lowercased() }
        case .alphaDesc: return filtered.sorted { $0.title.lowercased() > $1.title.lowercased() }
        }
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
                                .listRowInsets(EdgeInsets())
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
                                                observable.viewModelRef.onDeleteOffer(offer: offer)
                                            } label: {
                                                Label("Supprimer", systemImage: "trash")
                                            }
                                        }
                                    }
                                }
                            }
                            .listStyle(.plain)
                            .scrollContentBackground(.hidden)   // laisse voir le fond appBackground (Scaffold Android)
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
                        // FAB Android : Text("+", headlineSmall), secondary / onSecondary
                        Text("+")
                            .appTextStyle(.headlineSmall)
                            .foregroundStyle(Color.onCoralSecondary)
                            .frame(width: 56, height: 56)
                            .background(Color.coralSecondary)
                            .clipShape(Circle())
                            .shadow(radius: 4)
                    }
                    .padding(20)
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
            .sheet(item: $offerBeingEdited) { offer in
                JobOfferFormSheet(existingOffer: offer, viewModel: observable.viewModelRef) {
                    offerBeingEdited = nil
                }
            }
        }
}
