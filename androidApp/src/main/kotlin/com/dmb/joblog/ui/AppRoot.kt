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
import com.dmb.joblog.ui.privacy.PrivacyScreen
import com.dmb.joblog.ui.settings.SettingsScreen

/** Écran superposé à la liste, accessible depuis l'icône ⚙️ de sa barre du haut : Réglages → À propos / Confidentialité. */
private enum class OverlayScreen { NONE, SETTINGS, ABOUT, PRIVACY }

/**
 * Racine de l'app : onboarding au premier lancement, sinon liste directement. Les deux ViewModels sont créés UNE fois par
 * `MainActivity` (le même `jobOfferListViewModel` sert au splash et à l'écran de liste : pas de double chargement).
 * « Réglages », « À propos » et « Politique de confidentialité » se superposent à la liste (qui reste composée dessous :
 * recherche, tri et défilement sont conservés) : Liste → Réglages → (À propos | Confidentialité), retour en cascade.
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun AppRoot(
    jobOfferListViewModel: JobOfferListViewModel,
    onboardingViewModel: OnboardingViewModel,
) {
    // Lu une seule fois ; sauvegardé pour survivre à une rotation pendant l'onboarding.
    var showOnboarding by rememberSaveable { mutableStateOf(!onboardingViewModel.hasCompletedOnboarding()) }
    var overlay by rememberSaveable { mutableStateOf(OverlayScreen.NONE) }

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
                JobOfferListScreen(viewModel = jobOfferListViewModel, onOpenSettings = { overlay = OverlayScreen.SETTINGS })
                // Motion Material 3 Expressive (ressorts) pour l'ouverture / fermeture de la pile Réglages uniquement.
                val motion = MotionScheme.expressive()
                AnimatedVisibility(
                    visible = overlay != OverlayScreen.NONE,
                    enter = slideInHorizontally(animationSpec = motion.defaultSpatialSpec()) { it } + fadeIn(motion.defaultEffectsSpec()),
                    exit = slideOutHorizontally(animationSpec = motion.defaultSpatialSpec()) { it } + fadeOut(motion.defaultEffectsSpec()),
                ) {
                    // Retour système : ferme À propos/Confidentialité vers Réglages, puis Réglages vers la liste.
                    BackHandler {
                        overlay = when (overlay) {
                            OverlayScreen.ABOUT, OverlayScreen.PRIVACY -> OverlayScreen.SETTINGS
                            else -> OverlayScreen.NONE
                        }
                    }
                    when (overlay) {
                        OverlayScreen.SETTINGS -> SettingsScreen(
                            onBack = { overlay = OverlayScreen.NONE },
                            onOpenAbout = { overlay = OverlayScreen.ABOUT },
                            onOpenPrivacy = { overlay = OverlayScreen.PRIVACY },
                        )
                        OverlayScreen.ABOUT -> AboutScreen(onBack = { overlay = OverlayScreen.SETTINGS })
                        OverlayScreen.PRIVACY -> PrivacyScreen(onBack = { overlay = OverlayScreen.SETTINGS })
                        OverlayScreen.NONE -> Unit
                    }
                }
            }
        }
    }
}
