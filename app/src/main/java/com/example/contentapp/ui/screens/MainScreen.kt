package com.example.contentapp.ui.screens

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.contentapp.R
import com.example.contentapp.ai.PromptExpansionResult
import com.example.contentapp.ui.theme.AccentBlue
import com.example.contentapp.ui.theme.AccentBlueContainer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Главный экран приложения (S1 из SPEC.md).
 * onExpandPrompt — реализация P1 (разворачивание промта); экран не знает, какая
 * именно ИИ-модель отвечает, только вызывает переданную suspend-функцию (см. AppNavigation.kt).
 */
@Composable
fun MainScreen(
    onSubmit: (String) -> Unit,
    onExpandPrompt: suspend (shortPrompt: String, photoBytes: ByteArray?) -> PromptExpansionResult
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var shortPrompt by remember { mutableStateOf("") }
    var expandedPrompt by remember { mutableStateOf<String?>(null) }
    var selectedPhotoUri by remember { mutableStateOf<Uri?>(null) }
    var thumbnailBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var isExpanding by remember { mutableStateOf(false) }
    var expansionError by remember { mutableStateOf<String?>(null) }

    // S1.B1 — системный выбор фото (Photo Picker), permission не требуется
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri -> selectedPhotoUri = uri }

    // Декодируем маленькую миниатюру при каждой смене выбранного фото — полноразмерный
    // Bitmap для превью не нужен, отдельная функция сэмплирует под нужный размер.
    LaunchedEffect(selectedPhotoUri) {
        val uri = selectedPhotoUri
        thumbnailBitmap = if (uri != null) decodeThumbnail(context, uri) else null
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {

            // S1.T0 — плашка с названием приложения (общий компонент, см. AppNameBanner.kt)
            AppNameBanner()

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {

                Spacer(Modifier.height(12.dp))

                Text(
                    text = stringResource(R.string.prompt_label),
                    style = MaterialTheme.typography.headlineSmall,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )

                Spacer(Modifier.height(12.dp))

                // S1.F1 — поле ввода промта, во всю ширину экрана
                OutlinedTextField(
                    value = shortPrompt,
                    onValueChange = { shortPrompt = it },
                    placeholder = { Text(stringResource(R.string.prompt_placeholder)) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = AccentBlueContainer,
                        unfocusedContainerColor = AccentBlueContainer
                    )
                )

                Spacer(Modifier.height(8.dp))

                // Ряд под полем ввода: кнопка-скрепка (S1.B1) + кнопка Enter (S1.B2),
                // одного размера с S1.B3/S1.B4 (44dp), равномерно делят ширину экрана.
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // S1.B1 — кнопка-скрепка, пока фото не выбрано.
                    // После выбора — превью фото вместо кнопки: тап по нему заново открывает
                    // пикер (замена), крестик сверху (S1.B8) убирает фото совсем.
                    if (selectedPhotoUri == null) {
                        Button(
                            onClick = {
                                photoPickerLauncher.launch(
                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                )
                            },
                            modifier = Modifier
                                .weight(1f)
                                .height(44.dp),
                            shape = RoundedCornerShape(16.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = AccentBlue),
                            contentPadding = PaddingValues(0.dp)
                        ) {
                            Icon(
                                Icons.Filled.AttachFile,
                                contentDescription = "Приложить фото",
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    } else {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(44.dp)
                        ) {
                            val bitmap = thumbnailBitmap
                            if (bitmap != null) {
                                Image(
                                    bitmap = bitmap.asImageBitmap(),
                                    contentDescription = "Выбранное фото",
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .clip(RoundedCornerShape(16.dp))
                                        .clickable {
                                            // Тап по превью — заменить фото (снова S1.B1)
                                            photoPickerLauncher.launch(
                                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                            )
                                        }
                                )
                            } else {
                                // Пока миниатюра декодируется — тот же контур, чтобы не прыгал layout
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .background(AccentBlueContainer, RoundedCornerShape(16.dp))
                                )
                            }
                            // S1.B8 — убрать фото совсем, без замены
                            IconButton(
                                onClick = {
                                    selectedPhotoUri = null
                                    thumbnailBitmap = null
                                },
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .padding(2.dp)
                                    .size(20.dp)
                                    .background(Color.White, RoundedCornerShape(50))
                            ) {
                                Icon(
                                    Icons.Filled.Close,
                                    contentDescription = "Убрать фото",
                                    tint = Color(0xFFB00020),
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                        }
                    }

                    // S1.B2 — Enter: вызывает P1 (разворачивание промта через ИИ)
                    Button(
                        onClick = {
                            if (shortPrompt.isBlank()) return@Button
                            coroutineScope.launch {
                                isExpanding = true
                                expansionError = null
                                val photoBytes = selectedPhotoUri?.let { readPhotoBytes(context, it) }
                                when (val result = onExpandPrompt(shortPrompt, photoBytes)) {
                                    is PromptExpansionResult.Success -> expandedPrompt = result.expandedPrompt
                                    is PromptExpansionResult.Failed -> expansionError = result.reason
                                }
                                isExpanding = false
                            }
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(44.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = AccentBlue),
                        contentPadding = PaddingValues(0.dp)
                    ) {
                        Icon(
                            Icons.Filled.ArrowForward,
                            contentDescription = "Enter",
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }

                if (expansionError != null) {
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = expansionError.orEmpty(),
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                // Блок "Я правильно поняла?" (S1.C1) появляется после успешного ответа P1
                expandedPrompt?.let { promptDraft ->
                    var editableText by remember { mutableStateOf(promptDraft) }

                    Spacer(Modifier.height(40.dp))
                    Text(
                        text = stringResource(R.string.confirm_label),
                        style = MaterialTheme.typography.titleMedium,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                    Spacer(Modifier.height(8.dp))

                    // S1.F2 — редактируемый развёрнутый промт, вдвое выше исходной высоты
                    OutlinedTextField(
                        value = editableText,
                        onValueChange = { editableText = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 140.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = AccentBlueContainer,
                            unfocusedContainerColor = AccentBlueContainer
                        )
                    )

                    Spacer(Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // S1.B3 — исходный текст (S1.F1), без правок
                        Button(
                            onClick = { onSubmit(shortPrompt) },
                            modifier = Modifier.weight(1f).height(44.dp),
                            shape = RoundedCornerShape(16.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = AccentBlue)
                        ) {
                            Text(stringResource(R.string.btn_as_i_want), fontWeight = FontWeight.Bold)
                        }
                        // S1.B4 — развёрнутый/отредактированный текст (S1.F2)
                        Button(
                            onClick = { onSubmit(editableText) },
                            modifier = Modifier.weight(1f).height(44.dp),
                            shape = RoundedCornerShape(16.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = AccentBlue)
                        ) {
                            Text(stringResource(R.string.btn_as_you_understood), fontWeight = FontWeight.Bold)
                        }
                    }
                }

                Spacer(Modifier.weight(1f))
            }
        }

        // Оверлей ожидания P1 — поверх текущего экрана, элементы за ним остаются видны
        if (isExpanding) {
            LoadingOverlay()
        }
    }
}

/** Читает байты выбранного фото (S1.B1) через ContentResolver, вне главного потока. */
private suspend fun readPhotoBytes(context: Context, uri: Uri): ByteArray? = withContext(Dispatchers.IO) {
    try {
        context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
    } catch (e: Exception) {
        null
    }
}

/**
 * Декодирует уменьшенную версию выбранного фото для превью на S1 —
 * не грузит полноразмерный Bitmap, только на несколько шагов sampleSize больше targetPx.
 * Два прохода по Uri: первый — только размеры (inJustDecodeBounds), второй — сам Bitmap.
 */
private suspend fun decodeThumbnail(context: Context, uri: Uri, targetPx: Int = 200): Bitmap? =
    withContext(Dispatchers.IO) {
        try {
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }

            var sampleSize = 1
            while (bounds.outWidth / (sampleSize * 2) >= targetPx && bounds.outHeight / (sampleSize * 2) >= targetPx) {
                sampleSize *= 2
            }

            val decodeOptions = BitmapFactory.Options().apply { inSampleSize = sampleSize }
            context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, decodeOptions) }
        } catch (e: Exception) {
            null
        }
    }
