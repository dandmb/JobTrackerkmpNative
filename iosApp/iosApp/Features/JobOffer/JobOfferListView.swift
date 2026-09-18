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

struct JobOfferListView: View {
    @StateObject private var observable: JobOfferListObservable
    @State private var showingAddSheet = false

    init(viewModel: JobOfferListViewModel) {
        _observable = StateObject(wrappedValue: JobOfferListObservable(viewModel: viewModel))
    }

    var body: some View {
        NavigationStack {
            Group {
                if observable.state.isLoading {
                    ProgressView("Chargement...")
                } else if observable.state.offers.isEmpty {
                    ContentUnavailableView(
                        "Aucune candidature",
                        systemImage: "briefcase",
                        description: Text("Ajoute ta première candidature avec le bouton +")
                    )
                } else {
                    List(observable.state.offers, id: \.id) { offer in
                        VStack(alignment: .leading) {
                            Text(offer.title).font(.headline)
                            Text(offer.company).font(.subheadline)
                        }
                    }
                }
            }
            .navigationTitle("Candidatures")
            .toolbar {
                ToolbarItem(placement: .primaryAction) {
                    Button(action: { showingAddSheet = true }) {
                        Image(systemName: "plus")
                    }
                }
            }
            .sheet(isPresented: $showingAddSheet) {
                AddJobOfferSheet(viewModel: observable.viewModelRef)
                    .presentationDetents([.medium])
            }
        }
    }
}
