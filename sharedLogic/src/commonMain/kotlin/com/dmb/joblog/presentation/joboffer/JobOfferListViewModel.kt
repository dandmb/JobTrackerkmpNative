package com.dmb.joblog.presentation.joboffer

import com.dmb.joblog.domain.model.ApplicationStatus
import com.dmb.joblog.domain.model.DeletedJobOffer
import com.dmb.joblog.domain.model.JobOffer
import com.dmb.joblog.domain.usecase.AddJobOfferUseCase
import com.dmb.joblog.domain.usecase.DeleteJobOfferUseCase
import com.dmb.joblog.domain.usecase.GetAllJobOffersUseCase
import com.dmb.joblog.domain.usecase.UpdateJobOfferUseCase
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.launch
import kotlinx.datetime.TimeZone
import kotlinx.datetime.todayIn
import com.rickclephas.kmp.nativecoroutines.NativeCoroutinesState

internal const val DEFAULT_ERROR_MESSAGE = "An error occurred, please try again."

private fun Throwable.messageOrDefault(): String = message?.takeIf { it.isNotBlank() } ?: DEFAULT_ERROR_MESSAGE

class JobOfferListViewModel internal constructor(
    private val getAllJobOffers: GetAllJobOffersUseCase,
    private val addJobOffer: AddJobOfferUseCase,
    private val updateJobOffer: UpdateJobOfferUseCase,
    private val deleteJobOffer: DeleteJobOfferUseCase,
    private val cleanUpAttachments: suspend () -> Unit = {},
) {
    private val viewModelScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    private val recentlyDeleted = LinkedHashMap<Long, DeletedJobOffer>()
    private val undoMutex = Mutex()

    private val _state = MutableStateFlow(JobOfferListState())

    @NativeCoroutinesState
    val state: StateFlow<JobOfferListState> = _state.asStateFlow()

    init {
        observeOffers()
        viewModelScope.launch {
            try {
                cleanUpAttachments()
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
            }
        }
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

    private fun launchReportingErrors(action: suspend () -> Unit) {
        viewModelScope.launch {
            try {
                action()
            } catch (e: CancellationException) {
                throw e   // annuler le scope (onCleared) ne doit jamais s'afficher comme une erreur
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
        launchReportingErrors {
            undoMutex.withLock {
                // Le geste « glisser pour supprimer » rappelle onDeleteOffer plusieurs fois pour une seule suppression
                // (constaté : 4 appels) : à partir du 2e, l'horodatage lu est déjà null et ne doit pas écraser celui mémorisé.
                val deleted = deleteJobOffer(offer)
                val known = recentlyDeleted[offer.id]
                if (deleted.createdAtEpochMillis != null || known == null) {
                    recentlyDeleted.remove(offer.id)
                    recentlyDeleted[offer.id] = deleted
                }
                while (recentlyDeleted.size > MAX_UNDOABLE_DELETIONS) recentlyDeleted.remove(recentlyDeleted.keys.first())
            }
        }
    }

    fun onRestoreOffer(offer: JobOffer) {
        launchReportingErrors {
            undoMutex.withLock {
                val deleted = recentlyDeleted.remove(offer.id)
                if (deleted != null) deleteJobOffer.restore(deleted)
                else deleteJobOffer.addIfMissing(offer)
            }
        }
    }

    fun onRestoreDeletedOffer(offerId: Long) {
        launchReportingErrors {
            undoMutex.withLock {
                val deleted = recentlyDeleted.remove(offerId) ?: return@withLock
                deleteJobOffer.restore(deleted)
            }
        }
    }

    fun onCleared() {
        viewModelScope.cancel()
    }
}

internal const val MAX_UNDOABLE_DELETIONS = 20
