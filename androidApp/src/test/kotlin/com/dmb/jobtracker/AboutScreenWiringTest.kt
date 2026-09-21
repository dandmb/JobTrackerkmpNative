package com.dmb.jobtracker

import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Garde-fous de l'écran « À propos » (lecture du source, comme `FormSheetUsesSharedLogicTest`).
 *
 * Deux familles :
 * 1. le câblage (le texte vient de sharedLogic, la version est lue dans la config de build, la suppression passe par le ViewModel) ;
 * 2. les FAITS sur lesquels s'appuie le texte de confidentialité (`AboutContent`) : si l'un d'eux devient faux (permission Internet,
 *    SDK d'analyse ou de pub, bibliothèque réseau…), ce test échoue pour rappeler de RELIRE le texte avant de continuer.
 */
class AboutScreenWiringTest {

    private fun read(vararg candidates: String): String = candidates.map(::File).first { it.exists() }.readText()

    private val aboutScreen = read(
        "src/main/kotlin/com/dmb/jobtracker/ui/about/AboutScreen.kt",
        "androidApp/src/main/kotlin/com/dmb/jobtracker/ui/about/AboutScreen.kt",
    )
    private val appVersion = read(
        "src/main/kotlin/com/dmb/jobtracker/ui/about/AppVersion.kt",
        "androidApp/src/main/kotlin/com/dmb/jobtracker/ui/about/AppVersion.kt",
    )
    private val listScreen = read(
        "src/main/kotlin/com/dmb/jobtracker/ui/joboffer/JobOfferListScreen.kt",
        "androidApp/src/main/kotlin/com/dmb/jobtracker/ui/joboffer/JobOfferListScreen.kt",
    )
    private val appRoot = read("src/main/kotlin/com/dmb/jobtracker/ui/AppRoot.kt", "androidApp/src/main/kotlin/com/dmb/jobtracker/ui/AppRoot.kt")
    private val manifest = read("src/main/AndroidManifest.xml", "androidApp/src/main/AndroidManifest.xml")
    private val versionCatalog = read("../gradle/libs.versions.toml", "gradle/libs.versions.toml")
    private val buildFiles = listOf(
        read("build.gradle.kts", "androidApp/build.gradle.kts"),
        read("../sharedLogic/build.gradle.kts", "sharedLogic/build.gradle.kts"),
    ).joinToString("\n")

    // ---------- câblage ----------

    @Test
    fun aboutScreen_takesItsTextFromTheSharedContentAndDoesNotHardCodeIt() {
        assertTrue(aboutScreen.contains("AboutContent.sections"))
        assertTrue(aboutScreen.contains("AboutContent.CONTACT_EMAIL") && aboutScreen.contains("AboutContent.CONTACT_LABEL"))
        assertTrue(aboutScreen.contains("AboutContent.firstConfirmation") && aboutScreen.contains("AboutContent.finalConfirmation"))
        assertFalse(aboutScreen.contains("Supprimer toutes"), "libellé de suppression codé en dur dans l'écran")
    }

    @Test
    fun version_isReadFromTheInstalledPackageNotHardCoded() {
        assertTrue(appVersion.contains("packageManager.getPackageInfo"))
        assertTrue(appVersion.contains("versionName"))
        assertTrue(appVersion.contains("getLongVersionCode"))
        assertFalse(Regex("""versionLabel\(\s*"[0-9]""").containsMatchIn(appVersion + aboutScreen), "version codée en dur")
        assertTrue(aboutScreen.contains("appVersionLabel("))
    }

    @Test
    fun deleteAll_goesThroughTheViewModelDoubleConfirmationOnly() {
        assertTrue(aboutScreen.contains("viewModel::onDeleteAllRequested"))
        assertTrue(aboutScreen.contains("viewModel::onDeleteAllFirstConfirmed"))
        assertTrue(aboutScreen.contains("viewModel::onDeleteAllFinalConfirmed"))
        assertTrue(aboutScreen.contains("viewModel::onDeleteAllCancelled"))
        assertTrue(aboutScreen.contains("DeleteAllStep.FIRST_CONFIRMATION") && aboutScreen.contains("DeleteAllStep.FINAL_CONFIRMATION"))
        assertFalse(aboutScreen.contains("DeleteAllJobOffersUseCase"), "l'écran ne doit pas appeler le use case directement")
        assertFalse(aboutScreen.contains("deleteAll()"), "l'écran ne doit pas appeler deleteAll() directement")
    }

    @Test
    fun deleteButton_usesTheErrorColors() {
        assertTrue(aboutScreen.contains("containerColor = MaterialTheme.colorScheme.error"))
        assertTrue(aboutScreen.contains("colorScheme.errorContainer"))
    }

    @Test
    fun listScreen_exposesAnAboutEntryPointInTheTopBar() {
        assertTrue(listScreen.contains("onOpenAbout"))
        assertTrue(listScreen.contains("AboutContent.ENTRY_POINT_LABEL"))
        assertTrue(appRoot.contains("onOpenAbout = { showAbout = true }"))
        assertTrue(appRoot.contains("AboutScreen(onBack = { showAbout = false })"))
        assertTrue(appRoot.contains("BackHandler"), "le bouton Retour système doit fermer l'écran")
    }

    @Test
    fun aboutScreen_mentionsNoLicenseNorThirdPartyElement() {
        // Décision explicite du propriétaire : aucune licence de dépendance ou de police à l'écran.
        val lower = aboutScreen.lowercase()
        listOf("licence", "license", "jakarta", "apache", "thirdparty", "open source").forEach {
            assertFalse(lower.contains(it), "« $it » dans l'écran À propos")
        }
    }

    private val contactIntentSource = read(
        "src/main/kotlin/com/dmb/jobtracker/ui/about/ContactIntent.kt",
        "androidApp/src/main/kotlin/com/dmb/jobtracker/ui/about/ContactIntent.kt",
    )

    @Test
    fun contact_opensTheMailAppWithSendToAndTheSharedMailtoUri() {
        assertTrue(contactIntentSource.contains("Intent.ACTION_SENDTO"))
        assertTrue(contactIntentSource.contains("AboutContent.contactMailtoUri()"))
        assertTrue(aboutScreen.contains("openMailApp("))
        assertTrue(aboutScreen.contains("AboutContent.CONTACT_NO_MAIL_APP_MESSAGE"), "sans app de messagerie, l'adresse doit s'afficher")
    }

    @Test
    fun contact_addressAndSubjectAreNotHardCodedOutsideTheSharedContent() {
        val sources = File(if (File("src/main").exists()) "src/main/kotlin" else "androidApp/src/main/kotlin")
            .walkTopDown().filter { it.extension == "kt" }.joinToString("\n") { it.readText() }

        assertFalse(sources.contains("bizwadan"), "adresse e-mail codée en dur : utiliser AboutContent.CONTACT_EMAIL")
        assertFalse(sources.contains("mailto:"), "URI mailto codée en dur : utiliser AboutContent.contactMailtoUri()")
        assertFalse(sources.contains("JobTracker - Contact"), "objet codé en dur : utiliser AboutContent.CONTACT_SUBJECT")
    }

    @Test
    fun contact_isCatchingActivityNotFoundInsteadOfCrashing() {
        assertTrue(contactIntentSource.contains("ActivityNotFoundException"))
    }

    // ---------- Material 3 Expressive : limité à l'écran « À propos » ----------

    private fun mainSources(): Map<String, String> =
        File(if (File("src/main").exists()) "src/main/kotlin" else "androidApp/src/main/kotlin")
            .walkTopDown().filter { it.extension == "kt" }.associate { it.name to it.readText() }

    @Test
    fun expressive_aboutScreenUsesTheExpressiveThemeAndComponents() {
        assertTrue(aboutScreen.contains("MaterialExpressiveTheme("))
        assertTrue(aboutScreen.contains("MotionScheme.expressive()"))
        assertTrue(aboutScreen.contains("LargeFlexibleTopAppBar("))
        assertTrue(aboutScreen.contains("ButtonDefaults.shapes()"))
        assertTrue(aboutScreen.contains("LoadingIndicator("))
        assertTrue(aboutScreen.contains("MaterialShapes.Cookie9Sided"))
        assertTrue(aboutScreen.contains("ExperimentalMaterial3ExpressiveApi"), "l'API Expressive exige un opt-in explicite")
    }

    @Test
    fun expressive_isNotAppliedToOtherScreensInThisChange() {
        // Périmètre demandé : « À propos » uniquement (le thème global de l'app n'est pas modifié).
        val allowed = setOf("AboutScreen.kt", "AppRoot.kt")   // AppRoot : seulement le motion d'ouverture de « À propos »
        mainSources().filterKeys { it !in allowed }.forEach { (name, text) ->
            listOf("MaterialExpressiveTheme", "MotionScheme.expressive", "LargeFlexibleTopAppBar", "ButtonDefaults.shapes", "LoadingIndicator")
                .forEach { assertFalse(text.contains(it), "« $it » dans $name : Expressive ne doit pas déborder de l'écran À propos") }
        }
        assertFalse(mainSources().getValue("AppRoot.kt").contains("MaterialExpressiveTheme"))
    }

    // ---------- faits sur lesquels repose le texte de confidentialité ----------

    @Test
    fun fact_manifestDeclaresNoPermissionAtAll() {
        assertFalse(manifest.contains("uses-permission"), "une permission a été ajoutée : relire AboutContent (« Ce que l'app ne fait pas »)")
        assertFalse(manifest.contains("INTERNET"), "permission Internet ajoutée : le texte « rien n'est envoyé » n'est plus vérifié")
    }

    @Test
    fun fact_noAnalyticsAdsOrNetworkLibraryIsDeclared() {
        val declared = (versionCatalog + "\n" + buildFiles).lowercase()
        listOf(
            "firebase", "crashlytics", "analytics", "admob", "play-services-ads", "sentry", "appsflyer", "mixpanel", "amplitude",
            "okhttp", "retrofit", "ktor", "volley", "coil", "glide", "apollo",
        ).forEach { assertFalse(declared.contains(it), "« $it » déclaré : relire AboutContent avant de continuer") }
    }

    @Test
    fun fact_theOnlyRuntimeDownloadIsTheGoogleFontsProvider() {
        val sources = File(if (File("src/main").exists()) "src/main/kotlin" else "androidApp/src/main/kotlin")
            .walkTopDown().filter { it.extension == "kt" }.joinToString("\n") { it.readText() }

        assertTrue(sources.contains("com.google.android.gms.fonts"), "la mention « police fournie par Google Play Services » doit rester vraie")
        listOf("java.net.", "HttpURLConnection", "URL(", "openConnection", "WebView").forEach {
            assertFalse(sources.contains(it), "accès réseau détecté (« $it ») : relire AboutContent")
        }
    }

    @Test
    fun fact_backupIsTheSystemDefault_soTheTextMentionsSystemBackups() {
        // allowBackup=true (défaut) : le système peut sauvegarder la base ; le texte l'annonce. Si cela change, relire la note.
        assertTrue(manifest.contains("allowBackup=\"true\""))
        assertEquals(1, Regex("allowBackup").findAll(manifest).count())
    }
}
