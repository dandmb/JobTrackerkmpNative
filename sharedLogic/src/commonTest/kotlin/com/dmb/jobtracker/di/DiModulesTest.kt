package com.dmb.jobtracker.di

import com.dmb.jobtracker.data.local.dao.JobOfferDao
import com.dmb.jobtracker.data.repository.JobOfferRepositoryImpl
import com.dmb.jobtracker.domain.repository.JobOfferRepository
import com.dmb.jobtracker.domain.usecase.AddJobOfferUseCase
import com.dmb.jobtracker.domain.usecase.DeleteJobOfferUseCase
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

    private fun koin(): Koin =
        koinApplication { modules(fakeDatabaseModule, repositoryModule, useCaseModule, viewModelModule) }.koin

    @Test
    fun sharedModules_lists_databaseRepositoryUseCaseAndViewModelModules() {
        val modules = sharedModules()

        assertEquals(4, modules.size)
        assertEquals(listOf(databaseModule, repositoryModule, useCaseModule, viewModelModule), modules)
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
}
