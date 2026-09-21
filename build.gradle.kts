plugins {
    // this is necessary to avoid the plugins to be loaded multiple times
    // in each subproject's classloader
    alias(libs.plugins.androidApplication) apply false
    alias(libs.plugins.androidMultiplatformLibrary) apply false
    alias(libs.plugins.composeMultiplatform) apply false
    alias(libs.plugins.composeCompiler) apply false
    alias(libs.plugins.kotlinMultiplatform) apply false
    alias(libs.plugins.ksp) apply false
    alias(libs.plugins.kmp.nativecoroutines) apply false
    alias(libs.plugins.kover)

}
// ---------------------------------------------------------------------------------------------
// Couverture de code (Kover) — rapport agrégé sharedLogic + androidApp
//   ./gradlew koverHtmlReport            -> build/reports/kover/html/index.html   (+ koverXmlReport -> build/reports/kover/report.xml)
//   ./gradlew koverHtmlReport -PkoverFull -> idem mais en incluant aussi les Composables (chiffre brut)
// Les filtres du rapport AGRÉGÉ se définissent ici (ceux des modules ne s'appliquent qu'à leur rapport propre).
// Exclus par défaut : code généré (Room), et rendu Compose / Activity / Application, qui ne sont pas testés
// unitairement (voir PROJECT_CONTEXT.md, section « Tests »).
// ---------------------------------------------------------------------------------------------
dependencies {
    kover(project(":sharedLogic"))
    kover(project(":androidApp"))
}

kover {
    reports {
        filters {
            excludes {
                classes(
                    "*_Impl",
                    "*_Impl\$*",
                    "*AppDatabaseConstructor*",
                    "*.BuildConfig",
                    "*.R",
                    "*.R\$*",
                )
                if (!providers.gradleProperty("koverFull").isPresent) {
                    annotatedBy("androidx.compose.runtime.Composable")
                    classes(
                        "*ComposableSingletons*",
                        "com.dmb.jobtracker.MainActivity*",
                        "com.dmb.jobtracker.MonApplication*",
                    )
                }
            }
        }
    }
}

// Point d'entrée unique pour les tests unitaires Kotlin. ATTENTION : `./gradlew test` ne lance PAS les tests de
// sharedLogic (un module Kotlin Multiplatform n'a pas de tâche `test`), d'où cette tâche.
// Les tests Swift se lancent à part : voir PROJECT_CONTEXT.md (section « Tests et couverture »).
tasks.register("allUnitTests") {
    group = "verification"
    description = "Tests unitaires Kotlin : sharedLogic (JVM Android + natif iOS Simulator) et androidApp."
    dependsOn(
        ":sharedLogic:testAndroidHostTest",
        ":sharedLogic:iosSimulatorArm64Test",
        ":androidApp:testDebugUnitTest",
    )
}
