package com.dmb.jobtracker

import android.os.Bundle
import android.os.SystemClock
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.dmb.jobtracker.presentation.joboffer.JobOfferListViewModel
import com.dmb.jobtracker.presentation.onboarding.OnboardingViewModel
import com.dmb.jobtracker.presentation.splash.SplashGating
import com.dmb.jobtracker.ui.AppRoot
import com.dmb.jobtracker.ui.theme.JobTrackerTheme
import org.koin.android.ext.android.inject

class MainActivity : ComponentActivity() {

    // Créés UNE seule fois ici : le ViewModel de la liste sert au gating du splash ET à l'écran principal (pas de rechargement).
    private val jobOfferListViewModel: JobOfferListViewModel by inject()
    private val onboardingViewModel: OnboardingViewModel by inject()

    override fun onCreate(savedInstanceState: Bundle?) {
        // Contrainte stricte de l'API : installSplashScreen() AVANT super.onCreate().
        val splashScreen = installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Le splash reste affiché tant que (durée minimale non écoulée) OU (liste en chargement) — règle partagée avec iOS.
        val startedAt = SystemClock.elapsedRealtime()
        splashScreen.setKeepOnScreenCondition {
            SplashGating.shouldKeepSplash(
                elapsedMillis = SystemClock.elapsedRealtime() - startedAt,
                isLoading = jobOfferListViewModel.state.value.isLoading,
            )
        }

        setContent {
            JobTrackerTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    AppRoot(jobOfferListViewModel, onboardingViewModel)
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        if (isFinishing) jobOfferListViewModel.onCleared()   // libère le scope du ViewModel quand l'activité se termine vraiment
    }
}

@Preview
@Composable
fun AppAndroidPreview() {
    App()
}
