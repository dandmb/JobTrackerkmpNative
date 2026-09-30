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
import com.dmb.joblog.ui.attachments.DocumentsScreen
import com.dmb.joblog.ui.joboffer.JobOfferListScreen
import com.dmb.joblog.ui.onboarding.OnboardingScreen
import com.dmb.joblog.ui.privacy.PrivacyScreen
import com.dmb.joblog.ui.settings.SettingsScreen

private enum class OverlayScreen { NONE, SETTINGS, DOCUMENTS, ABOUT, PRIVACY }

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun AppRoot(
    jobOfferListViewModel: JobOfferListViewModel,
    onboardingViewModel: OnboardingViewModel,
) {
    var showOnboarding by rememberSaveable { mutableStateOf(!onboardingViewModel.hasCompletedOnboarding()) }
    var overlay by rememberSaveable { mutableStateOf(OverlayScreen.NONE) }

    Crossfade(targetState = showOnboarding, label = "onboardingToMain") { onboarding ->
        if (onboarding) {
            OnboardingScreen(
                onFinished = {
                    onboardingViewModel.completeOnboarding()
                    showOnboarding = false
                }
            )
        } else {
            Box {
                JobOfferListScreen(viewModel = jobOfferListViewModel, onOpenSettings = { overlay = OverlayScreen.SETTINGS })
                val motion = MotionScheme.expressive()
                AnimatedVisibility(
                    visible = overlay != OverlayScreen.NONE,
                    enter = slideInHorizontally(animationSpec = motion.defaultSpatialSpec()) { it } + fadeIn(motion.defaultEffectsSpec()),
                    exit = slideOutHorizontally(animationSpec = motion.defaultSpatialSpec()) { it } + fadeOut(motion.defaultEffectsSpec()),
                ) {
                    BackHandler {
                        overlay = when (overlay) {
                            OverlayScreen.DOCUMENTS, OverlayScreen.ABOUT, OverlayScreen.PRIVACY -> OverlayScreen.SETTINGS
                            else -> OverlayScreen.NONE
                        }
                    }
                    when (overlay) {
                        OverlayScreen.SETTINGS -> SettingsScreen(
                            onBack = { overlay = OverlayScreen.NONE },
                            onOpenDocuments = { overlay = OverlayScreen.DOCUMENTS },
                            onOpenAbout = { overlay = OverlayScreen.ABOUT },
                            onOpenPrivacy = { overlay = OverlayScreen.PRIVACY },
                        )
                        OverlayScreen.DOCUMENTS -> DocumentsScreen(onBack = { overlay = OverlayScreen.SETTINGS })
                        OverlayScreen.ABOUT -> AboutScreen(onBack = { overlay = OverlayScreen.SETTINGS })
                        OverlayScreen.PRIVACY -> PrivacyScreen(onBack = { overlay = OverlayScreen.SETTINGS })
                        OverlayScreen.NONE -> Unit
                    }
                }
            }
        }
    }
}
