package com.example.contentapp.ui.screens

import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.contentapp.R
import com.example.contentapp.ui.theme.*

/**
 * Экран результата ("карточка в контексте"): полный промт виден целиком.
 *
 * resultImageBytes — байты изображения (см. content/ImageGenerationService.kt).
 * null, пока идёт генерация (см. isLoading в AppNavigation.kt) или если она не удалась.
 * contentErrorMessage — текст ошибки генерации, если она произошла.
 * saveMessage — результат нажатия "Сохранить" (S2.B1): текст для показа под кнопками,
 * null, пока сохранение не запускали. saveIsError — цвет текста (ошибка/успех).
 * См. content/ImageSaver.kt.
 */
@Composable
fun ResultScreen(
    fullPrompt: String,
    resultImageBytes: ByteArray?,
    contentErrorMessage: String?,
    saveMessage: String?,
    saveIsError: Boolean,
    onSave: () -> Unit,
    onNewRequest: () -> Unit
) {
    Column(modifier = Modifier.fillMaxSize()) {
        AppNameBanner()

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(20.dp)
        ) {
        // Полный текст промта — не сворачивается
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(AccentBlueContainer, RoundedCornerShape(16.dp))
                .padding(16.dp)
        ) {
            Text(fullPrompt, style = MaterialTheme.typography.bodyMedium)
        }

        Spacer(Modifier.height(16.dp))
        Text("Готово!", style = MaterialTheme.typography.headlineSmall)
        Spacer(Modifier.height(12.dp))

        // Карточка результата
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .background(AccentBlueContainer, RoundedCornerShape(16.dp)),
            contentAlignment = Alignment.Center
        ) {
            when {
                contentErrorMessage != null -> Text(
                    contentErrorMessage,
                    color = MaterialTheme.colorScheme.error,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                    modifier = Modifier.padding(16.dp)
                )
                resultImageBytes != null -> {
                    val bitmap = remember(resultImageBytes) {
                        BitmapFactory.decodeByteArray(resultImageBytes, 0, resultImageBytes.size)
                    }
                    if (bitmap != null) {
                        Image(
                            bitmap = bitmap.asImageBitmap(),
                            contentDescription = "Результат",
                            contentScale = ContentScale.Fit,
                            modifier = Modifier.fillMaxSize().padding(8.dp)
                        )
                    } else {
                        Text("Не удалось декодировать изображение")
                    }
                }
                else -> Text("Генерация...")  // на практике перекрыто LoadingOverlay из AppNavigation.kt
            }
        }

        Spacer(Modifier.height(16.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            Button(
                onClick = onSave,
                modifier = Modifier.weight(1f).height(48.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = AccentBlue)
            ) { Text(stringResource(R.string.btn_save), fontWeight = FontWeight.Bold) }

            OutlinedButton(
                onClick = onNewRequest,
                modifier = Modifier.weight(1f).height(48.dp),
                shape = RoundedCornerShape(16.dp)
            ) { Text(stringResource(R.string.btn_new_request)) }
        }

        if (saveMessage != null) {
            Spacer(Modifier.height(8.dp))
            Text(
                saveMessage,
                style = MaterialTheme.typography.bodySmall,
                color = if (saveIsError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        }

        }
    }
}
