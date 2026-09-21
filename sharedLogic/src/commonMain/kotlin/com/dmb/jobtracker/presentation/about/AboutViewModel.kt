package com.dmb.jobtracker.presentation.about

import com.dmb.jobtracker.domain.usecase.DeleteAllJobOffersUseCase
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

/**
 * Étape de la double confirmation de « Supprimer toutes mes données ». Ordre imposé :
 * `IDLE` → `FIRST_CONFIRMATION` → `FINAL_CONFIRMATION` → suppression. Annuler à n'importe quelle étape revient à `IDLE`.
 */
enum class DeleteAllStep { IDLE, FIRST_CONFIRMATION, FINAL_CONFIRMATION }

data class AboutState(
    val deleteStep: DeleteAllStep = DeleteAllStep.IDLE,
    val isDeleting: Boolean = false,
    /** Vrai une fois la suppression réussie ; repasse à faux dès qu'une nouvelle demande de suppression commence. */
    val dataDeleted: Boolean = false,
    val errorMessage: String? = null,
)

/**
 * ViewModel de l'écran « À propos ». Kotlin pur (comme les autres) : consommable depuis Swift, scope annulé via [onCleared].
 * La suppression n'a lieu qu'à la fin de la double confirmation : aucune méthode ne l'exécute directement.
 */
class AboutViewModel internal constructor(
    private val deleteAllJobOffers: DeleteAllJobOffersUseCase,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    private val _state = MutableStateFlow(AboutState())

    @NativeCoroutinesState
    val state: StateFlow<AboutState> = _state.asStateFlow()

    /** Tap sur « Supprimer toutes mes données » : ouvre la première confirmation. Sans effet pendant une suppression. */
    fun onDeleteAllRequested() {
        val current = _state.value
        if (current.isDeleting) return
        _state.value = current.copy(deleteStep = DeleteAllStep.FIRST_CONFIRMATION, dataDeleted = false, errorMessage = null)
    }

    /** « Continuer » dans la première confirmation : passe à la confirmation finale. Ignoré à toute autre étape. */
    fun onDeleteAllFirstConfirmed() {
        val current = _state.value
        if (current.deleteStep != DeleteAllStep.FIRST_CONFIRMATION || current.isDeleting) return
        _state.value = current.copy(deleteStep = DeleteAllStep.FINAL_CONFIRMATION)
    }

    /** « Annuler » (ou fermeture de la boîte de dialogue) : rien n'est supprimé. */
    fun onDeleteAllCancelled() {
        val current = _state.value
        if (current.isDeleting) return
        _state.value = current.copy(deleteStep = DeleteAllStep.IDLE)
    }

    /** Confirmation finale : la SEULE voie qui vide la base. Ignorée si la première confirmation n'a pas eu lieu. */
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
                _state.value = _state.value.copy(
                    isDeleting = false,
                    errorMessage = e.message?.takeIf { it.isNotBlank() } ?: DELETE_ALL_ERROR_MESSAGE,
                )
            }
        }
    }

    fun onCleared() {
        scope.cancel()
    }
}

/** Message affiché quand la suppression échoue sans message exploitable. */
internal const val DELETE_ALL_ERROR_MESSAGE = "La suppression a échoué. Tes données n'ont pas été supprimées, réessaie."
