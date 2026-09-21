package com.dmb.joblog.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.layout.Box
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MotionScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import com.dmb.joblog.presentation.joboffer.JobOfferListViewModel
import com.dmb.joblog.presentation.onboarding.OnboardingViewModel
import com.dmb.joblog.ui.about.AboutScreen
import com.dmb.joblog.ui.joboffer.JobOfferListScreen
import com.dmb.joblog.ui.onboarding.OnboardingScreen

/**
 * Racine de l'app : onboarding au premier lancement, sinon liste directement. Les deux ViewModels sont créés UNE fois par
 * `MainActivity` (le même `jobOfferListViewModel` sert au splash et à l'écran de liste : pas de double chargement).
 * L'écran « À propos » se superpose à la liste (qui reste composée dessous : recherche, tri et défilement sont conservés).
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun AppRoot(
    jobOfferListViewModel: JobOfferListViewModel,
    onboardingViewModel: OnboardingViewModel,
) {
    // Lu une seule fois ; sauvegardé pour survivre à une rotation pendant l'onboarding.
    var showOnboarding by rememberSaveable { mutableStateOf(!onboardingViewModel.hasCompletedOnboarding()) }
    var showAbout by rememberSaveable { mutableStateOf(false) }

    Crossfade(targetState = showOnboarding, label = "onboardingToMain") { onboarding ->
        if (onboarding) {
            OnboardingScreen(
                onFinished = {
                    onboardingViewModel.completeOnboarding()   // « Passer » comme « Commencer » : ne plus le montrer
                    showOnboarding = false                     // navigation sans redémarrer l'app
                }
            )
        } else {
            Box {
                JobOfferListScreen(viewModel = jobOfferListViewModel, onOpenAbout = { showAbout = true })
                // Motion Material 3 Expressive (ressorts) pour l'ouverture / fermeture de « À propos » uniquement.
                val motion = MotionScheme.expressive()
                AnimatedVisibility(
                    visible = showAbout,
                    enter = slideInHorizontally(animationSpec = motion.defaultSpatialSpec()) { it } + fadeIn(motion.defaultEffectsSpec()),
                    exit = slideOutHorizontally(animationSpec = motion.defaultSpatialSpec()) { it } + fadeOut(motion.defaultEffectsSpec()),
                ) {
                    BackHandler { showAbout = false }
                    AboutScreen(onBack = { showAbout = false })
                }
            }
        }
    }
}
