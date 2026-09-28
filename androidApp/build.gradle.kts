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

// Build « staging » (CD de distribution bêta, .github/workflows/cd-android-staging.yml) : tout vient de variables
// d'environnement, absentes en local et dans la CI de tests → aucune erreur Gradle, l'APK staging est alors simplement non signé.
fun envOrNull(name: String): String? = providers.environmentVariable(name).orNull?.takeIf { it.isNotBlank() }
val stagingKeystorePath = envOrNull("STAGING_KEYSTORE_PATH")
val stagingKeystorePassword = envOrNull("STAGING_KEYSTORE_PASSWORD")
val stagingKeyAlias = envOrNull("STAGING_KEY_ALIAS")
val hasStagingSigning = stagingKeystorePath != null && stagingKeystorePassword != null && stagingKeyAlias != null

android {
    namespace = "com.dmb.joblog"
    compileSdk = libs.versions.android.compileSdk.get().toInt()

    defaultConfig {
        applicationId = "com.dmb.joblog"
        minSdk = libs.versions.android.minSdk.get().toInt()
        targetSdk = libs.versions.android.targetSdk.get().toInt()
        // VERSION_CODE (github.run_number en CD) ; 1 par défaut en local.
        versionCode = envOrNull("VERSION_CODE")?.toInt() ?: 1
        versionName = "1.0"
        buildConfigField("boolean", "IS_STAGING", "false")
    }
    signingConfigs {
        if (hasStagingSigning) {
            create("staging") {
                storeFile = file(stagingKeystorePath!!)
                storePassword = stagingKeystorePassword
                keyAlias = stagingKeyAlias
                keyPassword = stagingKeystorePassword   // mot de passe de la clé = celui du keystore
            }
        }
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
        // Basé sur release ; com.dmb.joblog.staging s'installe à côté de la prod. Non signé si les variables STAGING_* manquent.
        create("staging") {
            initWith(getByName("release"))
            applicationIdSuffix = ".staging"
            signingConfig = signingConfigs.findByName("staging")
            matchingFallbacks += "release"   // les modules dépendants n'ont pas de build type « staging »
            buildConfigField("boolean", "IS_STAGING", "true")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    buildFeatures {
        compose = true
        buildConfig = true
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
