
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
                print("State observation error: \(error)")
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
    @State private var selectedStatuses: Set<ApplicationStatus> = []

    init(viewModel: JobOfferListViewModel) {
        _observable = StateObject(wrappedValue: JobOfferListObservable(viewModel: viewModel))
    }

    private var visibleOffers: [JobOffer] {
        observable.state.offers.filteredForDisplay(query: searchQuery, sortOption: sortOption, selectedStatuses: selectedStatuses)
    }

    var body: some View {
            NavigationStack {
                ZStack(alignment: .bottomTrailing) {
                    Group {
                        if observable.state.isLoading {
                            ProgressView()
                        } else if observable.state.offers.isEmpty {
                            ContentUnavailableView {
                                Label {
                                    Text("list_empty_title")
                                        .appTextStyle(.titleMedium)
                                        .foregroundStyle(Color.onAppBackground)
                                } icon: {
                                    Image(systemName: "tray")
                                        .foregroundStyle(Color.onSurfaceVariant)
                                }
                            } description: {
                                Text("list_empty_subtitle")
                                    .appTextStyle(.bodyMedium)
                                    .foregroundStyle(Color.onSurfaceVariant)
                                    .multilineTextAlignment(.center)
                            }
                        } else {
                            List {
                                Section {
                                    StatusFilterRow(selectedStatuses: $selectedStatuses)
                                }
                                .listRowInsets(EdgeInsets())
                                .listRowSeparator(.hidden)
                                .listRowBackground(Color.clear)

                                Section {
                                    JobOfferStatsCard(offers: observable.state.offers)
                                }
                                .listRowInsets(EdgeInsets(top: 8, leading: 16, bottom: 8, trailing: 16))
                                .listRowSeparator(.hidden)
                                .listRowBackground(Color.clear)

                                if visibleOffers.isEmpty {
                                    Text(selectedStatuses.isEmpty ? L("list_no_results", searchQuery) : L("list_no_results_filter"))
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
                                                offerPendingDeletion = offer
                                            } label: {
                                                Label("delete_action", systemImage: "trash")
                                            }
                                        }
                                    }
                                }
                            }
                            .listStyle(.plain)
                            .scrollContentBackground(.hidden)
                            .contentMargins(.bottom, 88, for: .scrollContent)
                            .searchable(
                                text: $searchQuery,
                                placement: .navigationBarDrawer(displayMode: .always),
                                prompt: "list_search_placeholder"
                            )
                        }
                    }
                    .frame(maxWidth: .infinity, maxHeight: .infinity)

                    Button(action: { showingAddSheet = true }) {
                        Image(systemName: "plus")
                            .font(.system(size: 20, weight: .semibold))
                            .foregroundStyle(Color.onCoralSecondary)
                            .frame(width: 56, height: 56)
                            .background(Color.coralSecondary)
                            .clipShape(Circle())
                            .shadow(radius: 4)
                    }
                    .accessibilityLabel("list_add")
                    .padding(20)
                    .accessibilityLabel("list_add")
                }
                .background(Color.appBackground.ignoresSafeArea())
                .navigationTitle("list_title")
                .navigationBarTitleDisplayMode(.large)
                .toolbar {
                    ToolbarItem(placement: .primaryAction) {
                        NavigationLink {
                            SettingsView()
                        } label: {
                            Image(systemName: "gearshape")
                        }
                        .accessibilityLabel(SettingsContent.companion.of(language: AppLanguage.current).entryPointLabel)
                    }
                    if !observable.state.offers.isEmpty {
                        ToolbarItem(placement: .primaryAction) {
                            Menu {
                                ForEach(SortOption.entries, id: \.self) { option in
                                    Button(option.titleKey) { sortOption = option }
                                }
                            } label: {
                                Image(systemName: "arrow.up.arrow.down")
                            }
                            .accessibilityLabel("list_sort")
                        }
                    }
                }
            }
            .tint(Color.tealPrimary)
            .onChange(of: observable.state.offers.isEmpty) { _, isEmpty in
                if isEmpty { selectedStatuses = [] }
            }
            .sheet(isPresented: $showingAddSheet) {
                JobOfferFormSheet(existingOffer: nil, viewModel: observable.viewModelRef) {
                    showingAddSheet = false
                }
            }
            .confirmationDialog(
                "list_delete_confirm_title",
                isPresented: Binding(
                    get: { offerPendingDeletion != nil },
                    set: { if !$0 { offerPendingDeletion = nil } }
                ),
                titleVisibility: .visible,
                presenting: offerPendingDeletion
            ) { offer in
                Button("delete_action", role: .destructive) {
                    observable.viewModelRef.onDeleteOffer(offer: offer)
                }
                Button("cancel", role: .cancel) {}
            } message: { offer in
                Text(L("list_delete_confirm_message", offer.title))
            }
            .sheet(item: $offerBeingEdited) { offer in
                JobOfferFormSheet(existingOffer: offer, viewModel: observable.viewModelRef) {
                    offerBeingEdited = nil
                }
            }
        }
}
