package com.dmb.joblog

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
import com.dmb.joblog.presentation.joboffer.JobOfferListViewModel
import com.dmb.joblog.presentation.onboarding.OnboardingViewModel
import com.dmb.joblog.presentation.splash.SplashGating
import com.dmb.joblog.ui.AppRoot
import com.dmb.joblog.ui.theme.JobLogTheme
import org.koin.android.ext.android.inject

class MainActivity : ComponentActivity() {

    private val jobOfferListViewModel: JobOfferListViewModel by inject()
    private val onboardingViewModel: OnboardingViewModel by inject()

    override fun onCreate(savedInstanceState: Bundle?) {
        // Contrainte de l'API : installSplashScreen() doit être appelé AVANT super.onCreate().
        val splashScreen = installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val startedAt = SystemClock.elapsedRealtime()
        splashScreen.setKeepOnScreenCondition {
            SplashGating.shouldKeepSplash(
                elapsedMillis = SystemClock.elapsedRealtime() - startedAt,
                isLoading = jobOfferListViewModel.state.value.isLoading,
            )
        }

        setContent {
            JobLogTheme {
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
        if (isFinishing) jobOfferListViewModel.onCleared()
    }
}

@Preview
@Composable
fun AppAndroidPreview() {
    App()
}
