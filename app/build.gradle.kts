import java.util.Properties
val localProperties = Properties()
val localPropertiesFile = rootProject.file("local.properties")
if (localPropertiesFile.exists()) {
    localProperties.load(localPropertiesFile.inputStream())
}
plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    // ИСПРАВЛЕНИЕ: Используем alias, как и для других плагинов
    alias(libs.plugins.ksp)
}
android {
    namespace = "com.example.warehousescanner"
    // ИСПРАВЛЕНИЕ: Используем стабильную версию SDK
    compileSdk = 34

    defaultConfig {
        applicationId = "com.example.warehousescanner"
        minSdk = 24
        // ИСПРАВЛЕНИЕ: Используем стабильную версию SDK
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        buildConfigField(
            "String", // Тип переменной
            "SALESDRIVE_API_KEY", // Название переменной в коде
            "\"${localProperties.getProperty("SALESDRIVE_API_KEY")}\"" // Значение из local.properties
        )
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
    buildFeatures {
        buildConfig = true
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlinOptions {
        jvmTarget = "11"
    }
}

dependencies {

    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.appcompat:appcompat:1.6.1")
    implementation("com.google.android.material:material:1.12.0")
    implementation("androidx.constraintlayout:constraintlayout:2.1.4")
    implementation(libs.androidx.activity)
    implementation(libs.androidx.navigation.fragment.ktx)
    implementation(libs.androidx.navigation.ui.ktx)
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")
    implementation("com.github.chrisbanes:PhotoView:2.3.0")
    // -- НАШИ ЗАВИСИМОСТИ НАЧИНАЮТСЯ ЗДЕСЬ --

    // Для сетевых запросов (Retrofit и Gson)
    implementation("com.squareup.retrofit2:retrofit:2.9.0")
    implementation("com.squareup.retrofit2:converter-gson:2.9.0")
    implementation("com.squareup.okhttp3:logging-interceptor:4.12.0")

    // ViewModel и LiveData для управления данными
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.8.0")
    implementation("androidx.lifecycle:lifecycle-livedata-ktx:2.8.0")
    implementation("androidx.activity:activity-ktx:1.9.0")
    implementation("io.coil-kt:coil:2.6.0")
    // Для списка (RecyclerView)
    implementation("androidx.recyclerview:recyclerview:1.3.2")

    // Для сканера штрих-кодов (CameraX + ML Kit)
    val cameraXVersion = "1.3.3"
    implementation("androidx.camera:camera-core:${cameraXVersion}")
    implementation("androidx.camera:camera-camera2:${cameraXVersion}")
    implementation("androidx.camera:camera-lifecycle:${cameraXVersion}")
    implementation("androidx.camera:camera-view:${cameraXVersion}")
    implementation("com.google.mlkit:barcode-scanning:17.2.0")
    val room_version = "2.6.1"
    implementation("androidx.room:room-runtime:$room_version")
    ksp("androidx.room:room-compiler:$room_version") // Используем ksp вместо kapt
    implementation("androidx.room:room-ktx:$room_version")

// --- GOOGLE GSON --- (для конвертации списков в JSON)
    implementation("com.google.code.gson:gson:2.10.1")
    implementation(libs.androidx.navigation.fragment.ktx)
    implementation(libs.androidx.navigation.ui.ktx)
// --- MATERIAL DESIGN --- (для BottomNavigationView)
    implementation("com.google.android.material:material:1.12.0")
    implementation(libs.androidx.fragment.ktx)
    // --- NAVIGATION COMPONENT ---
    implementation(libs.androidx.navigation.fragment.ktx)
    implementation(libs.androidx.navigation.ui.ktx)
}