package com.dmb.jobtracker.ui

import androidx.compose.animation.Crossfade
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import com.dmb.jobtracker.presentation.joboffer.JobOfferListViewModel
import com.dmb.jobtracker.presentation.onboarding.OnboardingViewModel
import com.dmb.jobtracker.ui.joboffer.JobOfferListScreen
import com.dmb.jobtracker.ui.onboarding.OnboardingScreen

/**
 * Racine de l'app : onboarding au premier lancement, sinon liste directement. Les deux ViewModels sont créés UNE fois par
 * `MainActivity` (le même `jobOfferListViewModel` sert au splash et à l'écran de liste : pas de double chargement).
 */
@Composable
fun AppRoot(
    jobOfferListViewModel: JobOfferListViewModel,
    onboardingViewModel: OnboardingViewModel,
) {
    // Lu une seule fois ; sauvegardé pour survivre à une rotation pendant l'onboarding.
    var showOnboarding by rememberSaveable { mutableStateOf(!onboardingViewModel.hasCompletedOnboarding()) }

    Crossfade(targetState = showOnboarding, label = "onboardingToMain") { onboarding ->
        if (onboarding) {
            OnboardingScreen(
                onFinished = {
                    onboardingViewModel.completeOnboarding()   // « Passer » comme « Commencer » : ne plus le montrer
                    showOnboarding = false                     // navigation sans redémarrer l'app
                }
            )
        } else {
            JobOfferListScreen(viewModel = jobOfferListViewModel)
        }
    }
}
