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


/**
 * Message de repli quand une exception capturée n'a pas de message exploitable (null ou blanc). Message TECHNIQUE (anglais,
 * comme les exceptions) : `state.errorMessage` n'est affiché par aucun écran pour l'instant ; s'il l'était un jour, il
 * faudrait le remplacer par un code d'erreur traduit par chaque écran.
 */
internal const val DEFAULT_ERROR_MESSAGE = "An error occurred, please try again."

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

    // Suppressions récentes, gardées EN MÉMOIRE le temps que l'utilisateur puisse les annuler (snackbar « Annuler »).
    // Elles portent l'horodatage de création d'origine, absent du modèle de domaine. Durée de vie = celle de ce ViewModel,
    // comme celle du snackbar qui déclenche l'annulation. Le Mutex ordonne suppression et restauration (FIFO) : une
    // annulation ne peut pas passer avant la fin de la suppression qu'elle annule.
    private val recentlyDeleted = LinkedHashMap<Long, DeletedJobOffer>()
    private val undoMutex = Mutex()

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
        launchReportingErrors {
            undoMutex.withLock {
                val deleted = deleteJobOffer(offer)
                // IDEMPOTENT : le geste « glisser pour supprimer » rappelle `onDeleteOffer` plusieurs fois pour une seule
                // suppression (constaté sur émulateur : 4 appels, `confirmValueChange` de Compose est rappelé). Dès le 2e appel
                // la ligne n'existe plus (horodatage lu = null) : ce résultat NE DOIT PAS écraser l'horodatage d'origine déjà
                // mémorisé, sinon « Annuler » restaurerait sans horodatage et l'offre remonterait en tête.
                val known = recentlyDeleted[offer.id]
                if (deleted.createdAtEpochMillis != null || known == null) {
                    recentlyDeleted.remove(offer.id)
                    recentlyDeleted[offer.id] = deleted
                }
                while (recentlyDeleted.size > MAX_UNDOABLE_DELETIONS) recentlyDeleted.remove(recentlyDeleted.keys.first())
            }
        }
    }

    /**
     * « Annuler » après une suppression : restaure l'offre avec ses valeurs d'origine, `createdAt` compris, donc à sa
     * position d'origine dans la liste (et non en tête). Peut s'appliquer à n'importe laquelle des suppressions récentes,
     * dans n'importe quel ordre. Sans suppression connue de ce ViewModel (cas qui ne se présente pas via l'interface :
     * le snackbar et ce ViewModel partagent la même durée de vie), retombe sur un ajout simple si l'offre est absente,
     * et ne fait rien si elle est déjà présente (double « Annuler »).
     */
    fun onRestoreOffer(offer: JobOffer) {
        launchReportingErrors {
            undoMutex.withLock {
                val deleted = recentlyDeleted.remove(offer.id)
                if (deleted != null) deleteJobOffer.restore(deleted)
                // Sans suppression connue : ajout simple SAUF si l'offre est déjà en base (double « Annuler ») :
                // ré-insérer la réhorodaterait et la ferait remonter en tête.
                else deleteJobOffer.addIfMissing(offer)
            }
        }
    }

    fun onCleared() {
        viewModelScope.cancel()
    }
}

/** Nombre de suppressions récentes pouvant encore être annulées (les plus anciennes sont oubliées). */
internal const val MAX_UNDOABLE_DELETIONS = 20
