package com.dmb.joblog

import android.os.Bundle
import android.os.SystemClock
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.dmb.joblog.presentation.joboffer.JobOfferListViewModel
import com.dmb.joblog.presentation.onboarding.OnboardingViewModel
import com.dmb.joblog.presentation.splash.SplashGating
import com.dmb.joblog.ui.AppRoot
import com.dmb.joblog.ui.theme.JobLogTheme
import org.koin.android.ext.android.get
import org.koin.android.ext.android.inject

class ListViewModelHolder(val viewModel: JobOfferListViewModel) : ViewModel() {
    override fun onCleared() {
        viewModel.onCleared()
    }
}

class MainActivity : ComponentActivity() {

    private val listViewModelHolder: ListViewModelHolder by viewModels {
        viewModelFactory { initializer { ListViewModelHolder(get()) } }
    }
    private val jobOfferListViewModel: JobOfferListViewModel get() = listViewModelHolder.viewModel
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
}
