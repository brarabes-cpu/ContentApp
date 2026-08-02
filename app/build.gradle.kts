import java.util.Properties

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

// Читаем local.properties — файл НЕ коммитится в git (см. .gitignore), там лежит
// GEMINI_API_KEY, чтобы ключ не попал в код/репозиторий.
val localProperties = Properties().apply {
    val file = rootProject.file("local.properties")
    if (file.exists()) load(file.inputStream())
}

android {
    namespace = "com.example.contentapp"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.example.contentapp"
        minSdk = 26
        targetSdk = 34
        versionCode = 1
        versionName = "0.1"

        // Пусто, если ключ не задан в local.properties — тогда GeminiPromptExpansionService
        // вернёт понятную ошибку вместо непонятного 401 от API.
        buildConfigField("String", "GEMINI_API_KEY", "\"${localProperties.getProperty("GEMINI_API_KEY", "")}\"")
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.14"
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
}

dependencies {
    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.2")
    implementation("androidx.activity:activity-compose:1.9.0")

    implementation(platform("androidx.compose:compose-bom:2024.06.00"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")

    implementation("androidx.navigation:navigation-compose:2.7.7")

    // HTTP-клиент для вызова ИИ-API (разворачивание промта и генерация изображения,
    // см. ai/GeminiPromptExpansionService.kt, content/BaseGeminiImageGenerationService.kt)
    implementation("com.squareup.okhttp3:okhttp:4.12.0")

    debugImplementation("androidx.compose.ui:ui-tooling")
}
