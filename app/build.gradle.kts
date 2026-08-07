import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.ksp)
}

// Release signing credentials live outside version control (keystore.properties
// is gitignored, and it only points at a keystore file kept outside the repo
// entirely — see AGENTS.md / README for how to set this up on a new machine).
// Falls back to an unsigned release build when the file is absent so a fresh
// clone still builds.
val keystorePropertiesFile = rootProject.file("keystore.properties")
val keystoreProperties = Properties()
if (keystorePropertiesFile.exists()) {
    keystoreProperties.load(keystorePropertiesFile.inputStream())
}

// --- Versioning -------------------------------------------------------
//
// Two independent numbers, don't conflate them:
//
// appVersionCode is Play's internal ordering key. Never shown to users.
// Must strictly increase on EVERY upload and can never be reused or
// lowered — Play rejects the upload outright. Bump it by 1 for each build
// sent to any track, even a throwaway one. Several uploads can share a
// versionName (successive alpha builds of 1.0.0) but never a versionCode.
//
// appVersionName is the human-readable string shown in the store listing
// and in Réglages. SemVer with an optional pre-release suffix:
//   1.0.0-alpha01 → 1.0.0-alpha02 → 1.0.0-beta01 → 1.0.0 → 1.0.1 → 1.1.0
// The suffix is documentation, not a mechanism: what actually decides who
// gets a build is the Play Console track it's uploaded to (Internal /
// Closed / Open / Production). Keeping the two aligned just avoids
// confusion about which build a tester is on.
//
// Deliberately not derived from `git rev-list --count`: the CI checkout is
// shallow, which would collapse the count to 1, and this repo's history
// has been lost once already.
val appVersionCode = 3
val appVersionName = "1.0.0-alpha03"

android {
    namespace = "com.sentinelle.app"
    compileSdk = 37

    defaultConfig {
        // Différent du `namespace` au-dessus, volontairement. Le namespace
        // est le package des sources (classes, R, BuildConfig) ; c'est
        // l'applicationId qui identifie l'app sur Play, et il est
        // définitif une fois la fiche créée.
        //
        // « com.sentinelle.app » était déjà pris sur Play. Celui-ci suit la
        // convention du DNS inversé appliquée à un domaine réellement
        // contrôlé — damso13-ux.github.io, qui héberge déjà la politique de
        // confidentialité. Le tiret disparaît : un segment de package Java
        // ne peut pas en contenir.
        applicationId = "io.github.damso13ux.sentinelle"
        minSdk = 29
        targetSdk = 37
        // Sentinelle is its own app identity now, distinct from the
        // Saracroche fork point — starting a clean version history rather
        // than carrying over Saracroche's 5.1.1/36. See the version block
        // at the top of this file for how these two are meant to move.
        versionCode = appVersionCode
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        versionName = appVersionName
    }

    signingConfigs {
        if (keystorePropertiesFile.exists()) {
            create("release") {
                storeFile = file(keystoreProperties["storeFile"] as String)
                storePassword = keystoreProperties["storePassword"] as String
                keyAlias = keystoreProperties["keyAlias"] as String
                keyPassword = keystoreProperties["keyPassword"] as String
            }
        }
    }
    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
            signingConfig =
                when {
                    keystorePropertiesFile.exists() -> signingConfigs.getByName("release")

                    // An unsigned release APK can't be installed, which makes
                    // it useless for the one thing a local release build is
                    // for: checking that R8 and the ProGuard rules didn't
                    // break anything at runtime. On a machine without the
                    // signing key, `-PuseDebugSigningForRelease` produces a
                    // fully minified, installable release build instead.
                    //
                    // Opt-in on purpose, never a silent fallback: the result
                    // is signed with the debug key and Play will reject it.
                    // It is for on-device testing only.
                    project.hasProperty("useDebugSigningForRelease") ->
                        signingConfigs.getByName("debug")

                    else -> null
                }
        }
        debug {
            isDebuggable = true
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    dependenciesInfo {
        includeInApk = false
        includeInBundle = false
    }
    buildFeatures {
        compose = true
        // Needed for BuildConfig.DEBUG, used to keep the debug-only Pro
        // unlock toggle (DebugSheet) out of release builds regardless of
        // how the sheet itself is reached.
        buildConfig = true
    }
    testOptions {
        unitTests {
            // Robolectric needs the merged resources/manifest to build its
            // simulated Android environment.
            isIncludeAndroidResources = true
        }
    }
    buildToolsVersion = "37.0.0"
}

kotlin {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_11)
    }
}

dependencies {

    implementation(libs.androidx.core.ktx)
    implementation(libs.material)

    // Jetpack Compose BOM
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.runtime.livedata)
    implementation(libs.androidx.material.icons.extended)
    implementation(libs.androidx.navigation.compose)

    // Gson for JSON parsing
    implementation(libs.gson)

    // DataStore for preferences
    implementation(libs.androidx.datastore.preferences)

    // Room database
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)

    // WorkManager
    implementation(libs.androidx.work.runtime.ktx)

    // Play Billing — single non-consumable "Sentinelle Pro" product
    implementation(libs.billing)

    // For debugging
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)

    testImplementation(libs.junit)
    testImplementation(libs.mockwebserver)
    testImplementation(libs.kotlinx.coroutines.test)
    // Robolectric: gives JVM unit tests a simulated Android Context, which
    // is what DataStore-backed PreferencesManager needs. Avoids having to
    // move these tests to a much slower instrumented (device) test run.
    testImplementation(libs.robolectric)
    testImplementation(libs.androidx.test.core)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
}
