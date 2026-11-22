plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ksp)
    alias(libs.plugins.room)
}

val firebaseEnabled = file("google-services.json").isFile
val crashProbe = providers.gradleProperty("crashProbe").orNull.toBoolean()
require(!crashProbe || firebaseEnabled) { "The crash probe requires app/google-services.json" }
if (firebaseEnabled) {
    apply(plugin = "com.google.gms.google-services")
    apply(plugin = "com.google.firebase.crashlytics")
}

android {
    namespace = "dev.biarko.stations"
    compileSdk = 36

    defaultConfig {
        applicationId = "dev.biarko.stations"
        minSdk = 24
        targetSdk = 36
        versionCode = 2
        versionName = "0.1.1"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        debug {
            val coverage = providers.gradleProperty("coverage").orNull.toBoolean()
            enableUnitTestCoverage = coverage
            enableAndroidTestCoverage = coverage
        }
        release {
            if (crashProbe) versionNameSuffix = "-probe"
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }

    if (crashProbe) {
        sourceSets.getByName("release") {
            java.srcDir("src/crashProbe/java")
            manifest.srcFile("src/crashProbe/AndroidManifest.xml")
        }
    }

    buildFeatures {
        compose = true
    }
    compileOptions {
        isCoreLibraryDesugaringEnabled = true
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

kotlin {
    jvmToolchain(17)
}

room {
    schemaDirectory("$projectDir/schemas")
}

dependencies {
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.compose.material3)
    implementation(libs.navigation3.ui)
    implementation(libs.lifecycle.navigation3)
    implementation(libs.lifecycle.runtime.compose)
    implementation(libs.ktor.client.okhttp)
    implementation(libs.kotlinx.serialization.json)
    coreLibraryDesugaring(libs.desugar)
    implementation(libs.room.runtime)
    ksp(libs.room.compiler)
    if (firebaseEnabled) {
        releaseImplementation(platform(libs.firebase.bom))
        releaseImplementation(libs.firebase.crashlytics)
    }

    testImplementation(libs.junit)
    testImplementation(libs.mockwebserver)
    androidTestImplementation(libs.androidx.test.runner)
    androidTestImplementation(libs.androidx.test.junit)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.compose.ui.test)
    debugImplementation(libs.compose.test.manifest)
}
