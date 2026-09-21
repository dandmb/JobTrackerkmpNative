import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidMultiplatformLibrary)
    alias(libs.plugins.ksp)
    alias(libs.plugins.androidx.room)
    alias(libs.plugins.kmp.nativecoroutines)
    alias(libs.plugins.kover)
}

kotlin {
    listOf(
        iosArm64(),
        iosSimulatorArm64()
    ).forEach { iosTarget ->
        iosTarget.binaries.framework {
            baseName = "SharedLogic"
            isStatic = true
        }
    }
    
    android {
       namespace = "com.dmb.joblog.sharedLogic"
       compileSdk = libs.versions.android.compileSdk.get().toInt()
       minSdk = libs.versions.android.minSdk.get().toInt()
    
       compilerOptions {
           jvmTarget = JvmTarget.JVM_11
       }
       androidResources {
           enable = true
       }
       withHostTest {
           isIncludeAndroidResources = true
       }
    }
    
    sourceSets {
        commonMain.dependencies {
            // put your Multiplatform dependencies here
            implementation(libs.androidx.room.runtime)
            implementation(libs.androidx.sqlite.bundled)
            implementation(libs.koin.core)
            implementation(libs.kotlinx.coroutines.core)
            implementation(libs.kotlinx.datetime)
            implementation(libs.multiplatform.settings.no.arg)   // persistance « onboarding déjà vu » (variante no-arg : aucun Context à câbler)
        }
        commonTest.dependencies {
            implementation(libs.kotlin.test)
            implementation(libs.kotlinx.coroutines.test)
            implementation(libs.turbine)
            implementation(libs.multiplatform.settings.test)      // MapSettings : Settings en mémoire pour les tests
        }
        androidMain.dependencies {
            implementation(libs.koin.android)
        }
        // Tests Room réels (DAO + migration) : SQLite natif sur iOS Simulator, sans appareil ni émulateur
        iosTest.dependencies {
            implementation(libs.androidx.room.testing)
        }
    }

    sourceSets.all {
        languageSettings.optIn("kotlin.experimental.ExperimentalObjCName")
    }
}
room {
    schemaDirectory("$projectDir/schemas")
}

// Le processus de test natif (simulateur) doit connaître le dossier des schémas Room exportés (MigrationTestHelper).
tasks.withType<org.jetbrains.kotlin.gradle.targets.native.tasks.KotlinNativeSimulatorTest>().configureEach {
    // `simctl spawn` ne relaie à l'app que les variables préfixées SIMCTL_CHILD_ (le préfixe est retiré côté processus)
    environment("SIMCTL_CHILD_ROOM_SCHEMA_DIR", "$projectDir/schemas")
}

nativeCoroutines {
    exposedSeverity = com.rickclephas.kmp.nativecoroutines.gradle.ExposedSeverity.ERROR
    // force à annoter tout Flow/suspend exposé, pour ne rien oublier — pratique courante en pro
}

dependencies {
    add("kspAndroid", libs.androidx.room.compiler)
    add("kspIosArm64", libs.androidx.room.compiler)
    add("kspIosSimulatorArm64", libs.androidx.room.compiler)
}
// Couverture (Kover) : on exclut le code généré (Room, KSP) qui n'est pas du code du projet ;
// il est couvert indirectement, via le Repository testé avec un DAO fake.
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
            }
        }
    }
}
