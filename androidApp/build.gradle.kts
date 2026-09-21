import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.kover)
}

kotlin {
    compilerOptions {
        jvmTarget = JvmTarget.JVM_11
    }
}
dependencies {
    implementation(project(":sharedUI"))
    implementation(libs.koin.android)
    implementation(libs.koin.core)
    implementation(libs.androidx.activity.compose)
    implementation(libs.koin.compose)

    implementation(libs.compose.uiToolingPreview)
    debugImplementation(libs.compose.uiTooling)

    implementation(libs.androidx.compose.material.icons)
    implementation(libs.compose.ui)
    implementation(libs.compose.material3)
    implementation(libs.kotlinx.datetime)
    implementation(libs.androidx.compose.ui.text.google.fonts)
    implementation(libs.androidx.core.splashscreen)

    testImplementation(libs.kotlin.testJunit)   // kotlin.test + annotation @Test JUnit 4
    testImplementation(libs.junit)
}

android {
    namespace = "com.dmb.joblog"
    compileSdk = libs.versions.android.compileSdk.get().toInt()

    defaultConfig {
        applicationId = "com.dmb.joblog"
        minSdk = libs.versions.android.minSdk.get().toInt()
        targetSdk = libs.versions.android.targetSdk.get().toInt()
        versionCode = 1
        versionName = "1.0"
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    buildFeatures {
        compose = true
    }
    // Seules l'anglais (par défaut) et le français sont gérés : les ressources des bibliothèques (Material, AndroidX…) dans
    // d'autres langues sont retirées de l'APK, donc une langue système « autre » retombe partout sur l'anglais (y compris
    // pour les textes fournis par les bibliothèques, ex. le sélecteur de date).
    androidResources {
        localeFilters += listOf("en", "fr")
    }
}
// Couverture (Kover) : par défaut on mesure la LOGIQUE de l'app (le rendu des Composables n'est pas testé unitairement).
// `./gradlew koverHtmlReport -PkoverFull` inclut aussi les Composables pour voir le chiffre brut.
kover {
    reports {
        filters {
            excludes {
                classes("*.BuildConfig", "*.R", "*.R\$*")
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
