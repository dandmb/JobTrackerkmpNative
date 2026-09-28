package com.dmb.joblog.presentation.about

import com.dmb.joblog.domain.usecase.DeleteAllJobOffersUseCase
import com.rickclephas.kmp.nativecoroutines.NativeCoroutinesState
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

enum class DeleteAllStep { IDLE, FIRST_CONFIRMATION, FINAL_CONFIRMATION }

data class AboutState(
    val deleteStep: DeleteAllStep = DeleteAllStep.IDLE,
    val isDeleting: Boolean = false,
    val dataDeleted: Boolean = false,
    val deletionFailed: Boolean = false,
)

class AboutViewModel internal constructor(
    private val deleteAllJobOffers: DeleteAllJobOffersUseCase,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    private val _state = MutableStateFlow(AboutState())

    @NativeCoroutinesState
    val state: StateFlow<AboutState> = _state.asStateFlow()

    fun onDeleteAllRequested() {
        val current = _state.value
        if (current.isDeleting) return
        _state.value = current.copy(deleteStep = DeleteAllStep.FIRST_CONFIRMATION, dataDeleted = false, deletionFailed = false)
    }

    fun onDeleteAllFirstConfirmed() {
        val current = _state.value
        if (current.deleteStep != DeleteAllStep.FIRST_CONFIRMATION || current.isDeleting) return
        _state.value = current.copy(deleteStep = DeleteAllStep.FINAL_CONFIRMATION)
    }

    fun onDeleteAllCancelled() {
        val current = _state.value
        if (current.isDeleting) return
        _state.value = current.copy(deleteStep = DeleteAllStep.IDLE)
    }

    fun onDeleteAllFinalConfirmed() {
        val current = _state.value
        if (current.deleteStep != DeleteAllStep.FINAL_CONFIRMATION || current.isDeleting) return
        _state.value = current.copy(deleteStep = DeleteAllStep.IDLE, isDeleting = true)
        scope.launch {
            try {
                deleteAllJobOffers()
                _state.value = _state.value.copy(isDeleting = false, dataDeleted = true)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _state.value = _state.value.copy(isDeleting = false, deletionFailed = true)
            }
        }
    }

    fun onCleared() {
        scope.cancel()
    }
}
