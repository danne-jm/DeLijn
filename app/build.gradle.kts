plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    id("com.google.devtools.ksp")
}

android {
    namespace = "com.danieljm.delijn"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.danieljm.delijn"
        minSdk = 26
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
}

dependencies {

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    // Navigation for Jetpack Compose
    implementation("androidx.navigation:navigation-compose:2.9.4")
    // Compose Destinations for type-safe navigation
    implementation("io.github.raamcosta.compose-destinations:core:2.2.0")
    ksp("io.github.raamcosta.compose-destinations:ksp:2.2.0")
    // Material icons (extended) for bottom navigation icons
    implementation("androidx.compose.material:material-icons-extended:1.7.8")
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)

    val room_version = "2.8.0"

    implementation("androidx.room:room-runtime:$room_version")

    ksp("androidx.room:room-compiler:$room_version")

    implementation("androidx.room:room-ktx:${room_version}")

    // Networking: Retrofit + Moshi
    implementation("com.squareup.retrofit2:retrofit:2.9.0")
    implementation("com.squareup.retrofit2:converter-moshi:2.9.0")
    implementation("com.squareup.moshi:moshi:1.15.0")
    implementation("com.squareup.moshi:moshi-kotlin:1.15.0")

    // OkHttp logging (use 4.x for Retrofit 2 compatibility)
    implementation("com.squareup.okhttp3:okhttp:4.10.0")
    implementation("com.squareup.okhttp3:logging-interceptor:4.10.0")

    // Coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")

    // Lifecycle ViewModel Compose
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.6.2")

    // Koin for DI (lightweight, no annotation processor required)
    implementation("io.insert-koin:koin-android:3.4.0")
    implementation("io.insert-koin:koin-androidx-compose:3.4.0")

    // Maps and location (osmdroid for map tiles without API key, and fused location)
    implementation("org.osmdroid:osmdroid-android:6.1.20")
    implementation("com.google.android.gms:play-services-location:21.3.0")

    // Gson (for parsing De Lijn's non-standard JSON responses)
    implementation("com.google.code.gson:gson:2.10.1")

}