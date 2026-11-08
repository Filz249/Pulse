plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt.android)
}

android {
    namespace = "com.pulse.music"
    compileSdk = 35
    defaultConfig {
        applicationId = "com.pulse.music"
        minSdk = 26
        targetSdk = 35
        val ciBuildNumber = System.getenv("GITHUB_RUN_NUMBER")?.toIntOrNull()?.coerceAtLeast(1) ?: 1
        versionCode = ciBuildNumber
        versionName = if (ciBuildNumber == 1) "1.0.0" else "1.0.$ciBuildNumber"
        val gitCommit = System.getenv("GITHUB_SHA") ?: "local"
        buildConfigField("String", "GIT_COMMIT", "\"$gitCommit\"")
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables { useSupportLibrary = true }
    }
    signingConfigs {
        create("pulseDistribution") {
            val keystorePath = System.getenv("PULSE_KEYSTORE_PATH")
            if (!keystorePath.isNullOrBlank()) {
                storeFile = file(keystorePath)
                storePassword = System.getenv("PULSE_KEYSTORE_PASSWORD")
                keyAlias = System.getenv("PULSE_KEY_ALIAS")
                keyPassword = System.getenv("PULSE_KEY_PASSWORD")
            }
        }
    }
    buildTypes {
        debug {
            // When a stable GitHub Actions keystore is configured, sign every distributed APK
            // with it so Android can update an existing installation in place.
            if (!System.getenv("PULSE_KEYSTORE_PATH").isNullOrBlank()) {
                signingConfig = signingConfigs.getByName("pulseDistribution")
            }
        }
        release {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }
    buildFeatures { compose = true; buildConfig = true }
    packaging { resources { excludes += "/META-INF/{AL2.0,LGPL2.1}" } }
}

configurations.all {
    resolutionStrategy.eachDependency {
        if (requested.group == "org.jetbrains.kotlin") useVersion("2.0.21")
    }
}

dependencies {
    implementation(platform("org.jetbrains.kotlin:kotlin-bom:2.0.21"))
    implementation(libs.androidx.core.ktx)
    implementation("androidx.core:core-splashscreen:1.0.1")
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.navigation.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons.extended)
    debugImplementation(libs.androidx.compose.ui.tooling)
    implementation(libs.androidx.media3.exoplayer)
    implementation(libs.androidx.media3.session)
    implementation(libs.androidx.media3.common)
    implementation(libs.androidx.media3.ui)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.kotlinx.coroutines.guava)
    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
    implementation(libs.androidx.hilt.navigation.compose)
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)
    implementation(libs.taglib)
    implementation(libs.coil.compose)
    implementation(libs.accompanist.permissions)
}

tasks.matching { it.name.contains("AarMetadata", ignoreCase = true) }.configureEach { enabled = false }
afterEvaluate { tasks.matching { it.name.contains("AarMetadata", ignoreCase = true) }.configureEach { enabled = false } }
