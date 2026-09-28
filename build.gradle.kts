plugins {
    // Nécessaire pour éviter que les plugins soient chargés plusieurs fois dans le classloader de chaque sous-projet.
    alias(libs.plugins.androidApplication) apply false
    alias(libs.plugins.androidMultiplatformLibrary) apply false
    alias(libs.plugins.composeMultiplatform) apply false
    alias(libs.plugins.composeCompiler) apply false
    alias(libs.plugins.kotlinMultiplatform) apply false
    alias(libs.plugins.ksp) apply false
    alias(libs.plugins.kmp.nativecoroutines) apply false
    alias(libs.plugins.kover)

}

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
                        "com.dmb.joblog.MainActivity*",
                        "com.dmb.joblog.MonApplication*",
                    )
                }
            }
        }
    }
}

// `./gradlew test` ne lance PAS les tests de sharedLogic (un module Kotlin Multiplatform n'a pas de tâche `test`), d'où cette tâche.
tasks.register("allUnitTests") {
    group = "verification"
    description = "Tests unitaires Kotlin : sharedLogic (JVM Android + natif iOS Simulator) et androidApp."
    dependsOn(
        ":sharedLogic:testAndroidHostTest",
        ":sharedLogic:iosSimulatorArm64Test",
        ":androidApp:testDebugUnitTest",
    )
}
