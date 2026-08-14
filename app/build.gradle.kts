import com.android.build.gradle.tasks.PackageAndroidArtifact

import java.util.Properties

// Signing credentials live in keystore.properties, which is gitignored. Without it the build
// still works and simply falls back to the default debug key, so a fresh clone is not blocked.
val keystoreProps = Properties().apply {
    val file = rootProject.file("keystore.properties")
    if (file.exists()) file.inputStream().use { load(it) }
}

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.jetbrains.kotlin.android)
}

android {
    namespace = "uk.akane.omni"
    compileSdk = 36

    androidResources {
        generateLocaleConfig = true
    }

    buildFeatures {
        buildConfig = true
    }

    signingConfigs {
        // Android keys app-update identity on the signing certificate, so debug and release
        // share one keystore. Otherwise a release build could never install over an existing
        // debug install and every update would mean uninstalling first, losing settings.
        val configure: com.android.build.api.dsl.ApkSigningConfig.() -> Unit = {
            if (keystoreProps.containsKey("storeFile")) {
                storeFile = file(keystoreProps.getProperty("storeFile"))
                storePassword = keystoreProps.getProperty("storePassword")
                keyAlias = keystoreProps.getProperty("keyAlias")
                keyPassword = keystoreProps.getProperty("keyPassword")
            }
        }
        getByName("debug", configure)
        create("release", configure)
    }

    defaultConfig {
        // Shipped under my own namespace so this fork is a distinct app rather than something
        // masquerading as the original, and so both can be installed at once. The Kotlin
        // namespace above stays as upstream has it: applicationId and namespace are separate
        // for exactly this reason, and renaming the source package would conflict on every
        // file the next time upstream is merged.
        applicationId = "dev.skye.omni"
        minSdk = 26
        targetSdk = 36
        versionCode = 7
        versionName = "1.6"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            signingConfig = signingConfigs.getByName("release")
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
        debug {
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
        freeCompilerArgs += listOf(
            "-Xno-param-assertions",
            "-Xno-call-assertions",
            "-Xno-receiver-assertions",
        )
    }

    // https://gitlab.com/IzzyOnDroid/repo/-/issues/491
    dependenciesInfo {
        includeInApk = false
        includeInBundle = false
    }

    // https://stackoverflow.com/a/77745844
    tasks.withType<PackageAndroidArtifact> {
        doFirst { appMetadata.asFile.orNull?.writeText("") }
    }

    lint {
        lintConfig = file("lint.xml")
    }
}

dependencies {

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.activity)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.androidx.preference.ktx)
    implementation(libs.androidx.core.splashscreen)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)

}