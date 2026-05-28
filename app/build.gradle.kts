plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    id("com.google.devtools.ksp")
}

android {
    namespace = "com.mappoint"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.mappoint"
        minSdk = 29
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
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
    }
    // Для поддержки vector drawables
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
            excludes += "/META-INF/gradle/incremental.annotation.processors"
        }
    }
}

dependencies {
    // ========== Основные зависимости (production) ==========
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.material.icons.extended)
    implementation(libs.play.services.location)
    implementation(libs.androidx.lifecycle.viewmodel.compose)

    // Navigation
    implementation("androidx.navigation:navigation-compose:2.9.7")

    // OSMdroid
    implementation("org.osmdroid:osmdroid-android:6.1.20")

    // Room
    implementation("androidx.room:room-runtime:2.8.4")
    implementation("androidx.room:room-ktx:2.8.4")
    implementation(libs.androidx.rules)
    ksp("androidx.room:room-compiler:2.8.4")

    // ========== Модульное тестирование ==========
    // JUnit 4
    testImplementation(libs.junit)

    // MockK для Kotlin
    testImplementation(libs.mockk)

    // Coroutines Test
    testImplementation(libs.kotlinx.coroutines.test)

    // AndroidX Core Testing
    testImplementation(libs.androidx.core.testing)

    // ========== Инструментальное тестирование (Android) ==========
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)

    // MockK для Android тестов
    androidTestImplementation(libs.mockk.android)

    // Debug зависимости
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)
}