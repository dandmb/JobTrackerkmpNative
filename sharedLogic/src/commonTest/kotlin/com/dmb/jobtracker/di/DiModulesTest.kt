package com.dmb.jobtracker.di

import com.dmb.jobtracker.data.local.dao.JobOfferDao
import com.dmb.jobtracker.data.local.OnboardingRepositoryImpl
import com.dmb.jobtracker.data.repository.JobOfferRepositoryImpl
import com.dmb.jobtracker.domain.repository.OnboardingRepository
import com.dmb.jobtracker.presentation.onboarding.OnboardingViewModel
import com.russhwolf.settings.MapSettings
import com.russhwolf.settings.Settings
import com.dmb.jobtracker.domain.repository.JobOfferRepository
import com.dmb.jobtracker.domain.usecase.AddJobOfferUseCase
import com.dmb.jobtracker.domain.usecase.DeleteAllJobOffersUseCase
import com.dmb.jobtracker.domain.usecase.DeleteJobOfferUseCase
import com.dmb.jobtracker.presentation.about.AboutViewModel
import com.dmb.jobtracker.domain.usecase.GetAllJobOffersUseCase
import com.dmb.jobtracker.domain.usecase.UpdateJobOfferUseCase
import com.dmb.jobtracker.presentation.joboffer.JobOfferListViewModel
import com.dmb.jobtracker.testutil.FakeJobOfferDao
import com.dmb.jobtracker.testutil.jobOffer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.koin.core.Koin
import org.koin.dsl.koinApplication
import org.koin.dsl.module
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotSame
import kotlin.test.assertSame
import kotlin.test.assertTrue
import kotlin.test.assertFalse

/**
 * Vérifie le graphe d'injection SANS base de données réelle : `databaseModule` (Room) est remplacé par un DAO fake,
 * tous les autres modules sont les vrais. Un binding manquant ou mal typé est ainsi détecté ici plutôt qu'au lancement de l'app.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class DiModulesTest {

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(StandardTestDispatcher())
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private val fakeDatabaseModule = module { single<JobOfferDao> { FakeJobOfferDao() } }

    /** Remplace le vrai `Settings()` (SharedPreferences / NSUserDefaults) par un Settings en mémoire. Doit venir APRÈS onboardingModule. */
    private val fakeSettingsModule = module { single<Settings> { MapSettings() } }

    private fun onboardingKoin(): Koin = koinApplication { modules(onboardingModule, fakeSettingsModule) }.koin

    private fun koin(): Koin =
        koinApplication { modules(fakeDatabaseModule, repositoryModule, useCaseModule, viewModelModule) }.koin

    @Test
    fun sharedModules_lists_databaseRepositoryUseCaseViewModelAndOnboardingModules() {
        val modules = sharedModules()

        assertEquals(5, modules.size)
        assertEquals(listOf(databaseModule, repositoryModule, useCaseModule, viewModelModule, onboardingModule), modules)
    }

    @Test
    fun repositoryModule_resolvesTheRealImplementationAsSingleton() {
        val koin = koin()

        val repository = koin.get<JobOfferRepository>()

        assertIs<JobOfferRepositoryImpl>(repository)
        assertSame(repository, koin.get<JobOfferRepository>())
    }

    @Test
    fun useCaseModule_resolvesEveryUseCase() {
        val koin = koin()

        koin.get<GetAllJobOffersUseCase>()
        koin.get<AddJobOfferUseCase>()
        koin.get<UpdateJobOfferUseCase>()
        koin.get<DeleteJobOfferUseCase>()
        koin.get<DeleteAllJobOffersUseCase>()
    }

    @Test
    fun useCaseModule_isFactory_eachResolutionCreatesANewInstance() {
        val koin = koin()

        assertNotSame(koin.get<AddJobOfferUseCase>(), koin.get<AddJobOfferUseCase>())
    }

    @Test
    fun viewModelModule_isFactory_eachResolutionCreatesANewViewModel() = runTest {
        val koin = koin()

        val first = koin.get<JobOfferListViewModel>()
        val second = koin.get<JobOfferListViewModel>()

        assertNotSame(first, second)
        first.onCleared()
        second.onCleared()
    }

    @Test
    fun viewModelFromKoin_isWiredEndToEndThroughRepositoryAndDao() = runTest {
        val koin = koin()
        val viewModel = koin.get<JobOfferListViewModel>()
        advanceUntilIdle()

        viewModel.onAddOffer(jobOffer(title = "Via Koin", company = "Acme"))
        advanceUntilIdle()

        assertEquals(listOf("Via Koin"), viewModel.state.value.offers.map { it.title })
        viewModel.onCleared()
    }

    // ---------- à propos ----------

    @Test
    fun viewModelModule_aboutViewModelIsAFactory_eachResolutionCreatesANewInstance() {
        val koin = koin()

        val first = koin.get<AboutViewModel>()
        val second = koin.get<AboutViewModel>()

        assertNotSame(first, second)
        first.onCleared()
        second.onCleared()
    }

    @Test
    fun aboutViewModelFromKoin_deletesEverythingThroughTheRealRepositoryAndDao() = runTest {
        // Instances NEUVES propres à ce test (DAO fake + repository réel) : en natif, les `single` des modules top-level
        // (`repositoryModule`) et de la classe de test survivent d'un test à l'autre et garderaient les données des autres tests.
        val koin = koinApplication {
            modules(
                module {
                    single<JobOfferDao> { FakeJobOfferDao() }
                    single<JobOfferRepository> { JobOfferRepositoryImpl(dao = get()) }
                },
                useCaseModule,
                viewModelModule,
            )
        }.koin
        val list = koin.get<JobOfferListViewModel>()
        val about = koin.get<AboutViewModel>()
        list.onAddOffer(jobOffer(title = "A supprimer", company = "Acme"))
        advanceUntilIdle()
        assertEquals(listOf("A supprimer"), list.state.value.offers.map { it.title })

        about.onDeleteAllRequested()
        about.onDeleteAllFirstConfirmed()
        about.onDeleteAllFinalConfirmed()
        advanceUntilIdle()

        assertTrue(list.state.value.offers.isEmpty())
        assertTrue(about.state.value.dataDeleted)
        list.onCleared()
        about.onCleared()
    }

    // ---------- onboarding ----------

    @Test
    fun onboardingModule_resolvesTheRealRepositoryAsSingleton() {
        val koin = onboardingKoin()

        val repository = koin.get<OnboardingRepository>()

        assertIs<OnboardingRepositoryImpl>(repository)
        assertSame(repository, koin.get<OnboardingRepository>())
    }

    @Test
    fun onboardingModule_viewModelIsAFactory_eachResolutionCreatesANewInstance() {
        val koin = onboardingKoin()

        assertNotSame(koin.get<OnboardingViewModel>(), koin.get<OnboardingViewModel>())
    }

    @Test
    fun onboardingViewModelFromKoin_persistsCompletionThroughTheSharedSettings() {
        val koin = onboardingKoin()
        val first = koin.get<OnboardingViewModel>()
        assertFalse(first.hasCompletedOnboarding())

        first.completeOnboarding()

        // Une AUTRE instance (relancement) voit la valeur : le Settings est un singleton partagé.
        assertTrue(koin.get<OnboardingViewModel>().hasCompletedOnboarding())
    }

    @Test
    fun onboardingModule_settingsIsASingleton() {
        val koin = onboardingKoin()

        assertSame(koin.get<Settings>(), koin.get<Settings>())
    }
}
