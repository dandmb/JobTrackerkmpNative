package com.dmb.joblog.di

import com.dmb.joblog.data.local.dao.JobOfferDao
import com.dmb.joblog.data.local.OnboardingRepositoryImpl
import com.dmb.joblog.data.repository.AttachmentRepositoryImpl
import com.dmb.joblog.data.repository.JobOfferRepositoryImpl
import com.dmb.joblog.domain.repository.OnboardingRepository
import com.dmb.joblog.presentation.onboarding.OnboardingViewModel
import com.russhwolf.settings.MapSettings
import com.russhwolf.settings.Settings
import com.dmb.joblog.domain.repository.AttachmentRepository
import com.dmb.joblog.domain.repository.JobOfferRepository
import com.dmb.joblog.domain.usecase.AddJobOfferUseCase
import com.dmb.joblog.domain.usecase.DeleteAllJobOffersUseCase
import com.dmb.joblog.domain.usecase.DeleteJobOfferUseCase
import com.dmb.joblog.presentation.about.AboutViewModel
import com.dmb.joblog.domain.usecase.GetAllJobOffersUseCase
import com.dmb.joblog.domain.usecase.UpdateJobOfferUseCase
import com.dmb.joblog.presentation.joboffer.JobOfferListViewModel
import com.dmb.joblog.data.files.AttachmentFileStore
import com.dmb.joblog.data.local.DatabaseTransaction
import com.dmb.joblog.data.local.DeletedDataPurger
import com.dmb.joblog.data.local.dao.AttachmentDao
import com.dmb.joblog.testutil.FakeAttachmentDao
import com.dmb.joblog.testutil.FakeAttachmentFileStore
import com.dmb.joblog.testutil.FakeJobOfferDao
import com.dmb.joblog.testutil.jobOffer
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

    private val fakeDatabaseModule = module {
        single<JobOfferDao> { FakeJobOfferDao() }
        single<AttachmentDao> { FakeAttachmentDao() }
        single<AttachmentFileStore> { FakeAttachmentFileStore() }
        single<DeletedDataPurger> { DeletedDataPurger {} }
        single<DatabaseTransaction> { DatabaseTransaction { it() } }
    }

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
    fun attachmentStartupCleanUp_runsOnlyOncePerKoinGraph_evenWhenTheListViewModelIsCreatedAgain() = runTest {
        // Les `single` des modules top-level gardent leur instance d'un graphe à l'autre : fermer un graphe les vide,
        // sinon ce test hériterait du nettoyage déjà fait (et du faux stockage) d'un test précédent.
        koin().close()
        val koin = koin()
        val store = koin.get<AttachmentFileStore>() as FakeAttachmentFileStore
        val leftover = store.writeTemporary(byteArrayOf(0))
        val first = koin.get<JobOfferListViewModel>()
        advanceUntilIdle()
        assertFalse(leftover in store.temporary, "le 1er ViewModel doit bien lancer le nettoyage de démarrage")
        val pendingImport = store.writeTemporary(byteArrayOf(1))

        val second = koin.get<JobOfferListViewModel>()
        advanceUntilIdle()

        assertTrue(pendingImport in store.temporary, "un 2e ViewModel (rotation Android) ne doit pas relancer le nettoyage")
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
        val koin = koinApplication {
            modules(
                module {
                    single<JobOfferDao> { FakeJobOfferDao() }
                    single<JobOfferRepository> { JobOfferRepositoryImpl(dao = get()) }
                    single<AttachmentRepository> { AttachmentRepositoryImpl(FakeAttachmentDao(), FakeAttachmentFileStore()) }
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

        assertTrue(koin.get<OnboardingViewModel>().hasCompletedOnboarding())
    }

    @Test
    fun onboardingModule_settingsIsASingleton() {
        val koin = onboardingKoin()

        assertSame(koin.get<Settings>(), koin.get<Settings>())
    }
}
