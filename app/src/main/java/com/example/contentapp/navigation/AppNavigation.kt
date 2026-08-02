package com.example.contentapp.navigation

import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.contentapp.ai.GeminiPromptExpansionService
import com.example.contentapp.ai.PromptExpansionService
import com.example.contentapp.content.GeminiFlashImageGenerationService
import com.example.contentapp.content.ImageGenerationResult
import com.example.contentapp.content.ImageGenerationService
import com.example.contentapp.content.ImageSaver
import com.example.contentapp.ui.screens.LoadingOverlay
import com.example.contentapp.ui.screens.MainScreen
import com.example.contentapp.ui.screens.ResultScreen
import kotlinx.coroutines.launch

/**
 * Навигация по экранам v1: главный экран -> экран результата.
 * Без регистрации, оплаты и уровней доступа (см. NEXT-STEPS.md — v0 с этим
 * функционалом сохранён отдельно в ContentApp_v0.zip).
 */
@Composable
fun AppNavigation() {
    val navController = rememberNavController()
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    // Единственная реализация P1 на сегодня — Gemini API (бесплатный тариф, текст + фото на входе).
    // MainScreen работает через интерфейс PromptExpansionService и не знает, какая это модель.
    val promptExpansionService: PromptExpansionService = remember { GeminiPromptExpansionService() }
    // Единственная модель генерации изображения в v1 (см. content/GeminiFlashImageGenerationService.kt).
    val imageGenerationService: ImageGenerationService = remember { GeminiFlashImageGenerationService() }

    var isLoading by remember { mutableStateOf(false) }
    var resultImageBytes by remember { mutableStateOf<ByteArray?>(null) }
    var contentErrorMessage by remember { mutableStateOf<String?>(null) }
    var saveMessage by remember { mutableStateOf<String?>(null) }
    var saveIsError by remember { mutableStateOf(false) }
    var lastPrompt by remember { mutableStateOf("") }

    // S2.B1 ("Сохранить") — на API 26-28 нужно runtime-разрешение на запись,
    // на API 29+ MediaStore работает без него (см. content/ImageSaver.kt).
    var pendingSaveBytes by remember { mutableStateOf<ByteArray?>(null) }
    val storagePermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        val bytes = pendingSaveBytes
        pendingSaveBytes = null
        if (granted && bytes != null) {
            coroutineScope.launch {
                when (val result = ImageSaver.saveToGallery(context, bytes)) {
                    is ImageSaver.Result.Success -> { saveMessage = "Сохранено в галерею"; saveIsError = false }
                    is ImageSaver.Result.Failed -> { saveMessage = result.reason; saveIsError = true }
                }
            }
        } else if (!granted) {
            saveMessage = "Нет разрешения на сохранение в галерею"
            saveIsError = true
        }
    }

    fun saveResultImage(bytes: ByteArray) {
        val needsPermission = Build.VERSION.SDK_INT < Build.VERSION_CODES.Q &&
            ContextCompat.checkSelfPermission(context, android.Manifest.permission.WRITE_EXTERNAL_STORAGE) !=
            android.content.pm.PackageManager.PERMISSION_GRANTED
        if (needsPermission) {
            pendingSaveBytes = bytes
            storagePermissionLauncher.launch(android.Manifest.permission.WRITE_EXTERNAL_STORAGE)
        } else {
            coroutineScope.launch {
                when (val result = ImageSaver.saveToGallery(context, bytes)) {
                    is ImageSaver.Result.Success -> { saveMessage = "Сохранено в галерею"; saveIsError = false }
                    is ImageSaver.Result.Failed -> { saveMessage = result.reason; saveIsError = true }
                }
            }
        }
    }

    NavHost(navController = navController, startDestination = "main") {

        composable("main") {
            MainScreen(
                onSubmit = { prompt ->
                    lastPrompt = prompt
                    resultImageBytes = null
                    contentErrorMessage = null
                    saveMessage = null
                    saveIsError = false
                    isLoading = true
                    navController.navigate("result")
                    coroutineScope.launch {
                        when (val result = imageGenerationService.generateImage(prompt)) {
                            is ImageGenerationResult.Success -> {
                                resultImageBytes = result.imageBytes
                            }
                            is ImageGenerationResult.Failed -> contentErrorMessage = result.reason
                        }
                        isLoading = false
                    }
                },
                onExpandPrompt = { shortPrompt, photoBytes ->
                    promptExpansionService.expandPrompt(shortPrompt, photoBytes)
                }
            )
        }

        composable("result") {
            ResultScreen(
                fullPrompt = lastPrompt,
                resultImageBytes = resultImageBytes,
                contentErrorMessage = contentErrorMessage,
                saveMessage = saveMessage,
                saveIsError = saveIsError,
                onSave = {
                    resultImageBytes?.let { saveResultImage(it) }
                },
                onNewRequest = {
                    saveMessage = null
                    saveIsError = false
                    navController.popBackStack()
                }
            )
            if (isLoading) {
                LoadingOverlay()
            }
        }
    }
}
