package com.dmb.jobtracker.presentation.joboffer

import com.dmb.jobtracker.domain.model.ApplicationStatus
import com.dmb.jobtracker.domain.model.JobOffer
import com.dmb.jobtracker.domain.usecase.AddJobOfferUseCase
import com.dmb.jobtracker.domain.usecase.DeleteJobOfferUseCase
import com.dmb.jobtracker.domain.usecase.GetAllJobOffersUseCase
import com.dmb.jobtracker.domain.usecase.UpdateJobOfferUseCase
import kotlinx.coroutines.CancellationException
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


/** Message affiché quand une exception capturée n'a pas de message exploitable (null ou blanc). */
internal const val DEFAULT_ERROR_MESSAGE = "Une erreur est survenue, réessaie."

private fun Throwable.messageOrDefault(): String = message?.takeIf { it.isNotBlank() } ?: DEFAULT_ERROR_MESSAGE

class JobOfferListViewModel internal constructor(
    private val getAllJobOffers: GetAllJobOffersUseCase,
    private val addJobOffer: AddJobOfferUseCase,
    private val updateJobOffer: UpdateJobOfferUseCase,
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
                _state.value = _state.value.copy(isLoading = false, errorMessage = e.messageOrDefault())
            }
            .launchIn(viewModelScope)
    }

    /**
     * Lance une action utilisateur dans le scope du ViewModel en rapportant toute erreur dans `state.errorMessage`
     * au lieu de la laisser s'échapper (une exception non capturée dans ce scope fait planter l'application).
     * `CancellationException` est relancée : annuler le scope (`onCleared`) n'est pas une erreur à afficher.
     */
    private fun launchReportingErrors(action: suspend () -> Unit) {
        viewModelScope.launch {
            try {
                action()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _state.value = _state.value.copy(errorMessage = e.messageOrDefault())
            }
        }
    }

    fun onAddOffer(offer: JobOffer) {
        launchReportingErrors { addJobOffer(offer) }
    }

    fun onUpdateOffer(offer: JobOffer) {
        launchReportingErrors { updateJobOffer(offer) }
    }

    fun onStatusChanged(offer: JobOffer, newStatus: ApplicationStatus) {
        launchReportingErrors { updateJobOffer(offer.copy(status = newStatus)) }
    }

    fun onDeleteOffer(offer: JobOffer) {
        launchReportingErrors { deleteJobOffer(offer) }
    }

    fun onCleared() {
        viewModelScope.cancel()
    }
}