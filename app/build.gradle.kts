plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.google.gms.google.services)
    alias(libs.plugins.kotlin.kapt)
    alias(libs.plugins.hilt.android)
    alias(libs.plugins.compose.compiler)
}

android {
    namespace = "com.aidlink"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.aidlink"
        minSdk = 24
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        
        val mapboxPublicToken: String = project.findProperty("MAPBOX_PUBLIC_TOKEN") as? String ?: ""
        manifestPlaceholders["MAPBOX_ACCESS_TOKEN"] = mapboxPublicToken
        resValue("string", "mapbox_access_token", mapboxPublicToken)
        buildConfigField("String", "MAPBOX_ACCESS_TOKEN", "\"$mapboxPublicToken\"")
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
    kotlinOptions {
        jvmTarget = "11"
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.11"
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.firebase.messaging.ktx)

    // ✅ Use the Compose BOM to manage and align all Compose library versions.
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.foundation)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons.extended)

    // Implementation for Coil (Image Loading)
    implementation(libs.coil.compose)

    // Implementations for Location & Permissions
    implementation(libs.play.services.location)
    implementation(libs.accompanist.permissions)

    // Firebase (using the BOM)
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.auth.ktx)
    implementation(libs.firebase.firestore.ktx)
    implementation(libs.firebase.storage.ktx)

    // MapBox
    implementation(libs.mapbox.compose)
    implementation(libs.geo)
    implementation(libs.mapbox.android)
    implementation(libs.mapbox.search.android)
    implementation(libs.mapbox.search.ui)
    implementation(libs.mapbox.place.autocomplete)

    // Hilt for Dependency Injection
    implementation(libs.hilt.android)
    implementation(libs.androidx.foundation)
    implementation(libs.androidx.material3)
    kapt(libs.hilt.compiler)
    implementation(libs.androidx.hilt.navigation.compose)

    // Navigation and ViewModel
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.navigation.compose)

    // Testing
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
}

kapt {
    correctErrorTypes = true
}