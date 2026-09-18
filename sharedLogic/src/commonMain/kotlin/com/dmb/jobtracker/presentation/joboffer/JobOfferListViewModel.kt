package com.dmb.jobtracker.presentation.joboffer

import com.dmb.jobtracker.domain.model.ApplicationStatus
import com.dmb.jobtracker.domain.model.JobOffer
import com.dmb.jobtracker.domain.usecase.AddJobOfferUseCase
import com.dmb.jobtracker.domain.usecase.DeleteJobOfferUseCase
import com.dmb.jobtracker.domain.usecase.GetAllJobOffersUseCase
import com.dmb.jobtracker.domain.usecase.UpdateJobOfferStatusUseCase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.launch
import kotlinx.datetime.TimeZone
import kotlinx.datetime.todayIn
import com.rickclephas.kmp.nativecoroutines.NativeCoroutinesState


class JobOfferListViewModel(
    private val getAllJobOffers: GetAllJobOffersUseCase,
    private val addJobOffer: AddJobOfferUseCase,
    private val updateStatus: UpdateJobOfferStatusUseCase,
    private val deleteJobOffer: DeleteJobOfferUseCase
) {
    // Pas d'androidx.lifecycle.ViewModel ici : on reste 100% Kotlin pur
    // pour que ce soit consommable nativement depuis Swift sans dépendance Android.
    private val viewModelScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    private val _state = MutableStateFlow(JobOfferListState())

    @NativeCoroutinesState
    val state: StateFlow<JobOfferListState> = _state.asStateFlow()

    init {
        observeOffers()
    }

    private fun observeOffers() {
        getAllJobOffers()
            .onEach { offers ->
                _state.value = _state.value.copy(offers = offers, isLoading = false, errorMessage = null)
            }
            .catch { e ->
                _state.value = _state.value.copy(isLoading = false, errorMessage = e.message)
            }
            .launchIn(viewModelScope)
    }

    fun onAddOffer(title: String, company: String, url: String?) {
        viewModelScope.launch {
            try {
                addJobOffer(
                    JobOffer(
                        title = title,
                        company = company,
                        url = url,
                        appliedDate = kotlin.time.Clock.System.todayIn(TimeZone.currentSystemDefault()),
                        status = ApplicationStatus.APPLIED
                    )
                )
            } catch (e: IllegalArgumentException) {
                _state.value = _state.value.copy(errorMessage = e.message)
            }
        }
    }

    fun onStatusChanged(offer: JobOffer, newStatus: ApplicationStatus) {
        viewModelScope.launch { updateStatus(offer, newStatus) }
    }

    fun onDeleteOffer(offer: JobOffer) {
        viewModelScope.launch { deleteJobOffer(offer) }
    }

    fun onCleared() {
        viewModelScope.cancel()
    }
}