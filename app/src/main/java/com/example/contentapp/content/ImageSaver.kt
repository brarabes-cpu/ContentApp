package com.example.contentapp.content

import android.content.ContentValues
import android.content.Context
import android.os.Build
import android.provider.MediaStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Сохраняет байты изображения в системную галерею через MediaStore — публичный
 * API, не требует прямого доступа к файловой системе (scoped storage-совместимо).
 * Реализует S2.B1 ("Сохранить") из SPEC.md.
 */
object ImageSaver {

    sealed class Result {
        object Success : Result()
        data class Failed(val reason: String) : Result()
    }

    suspend fun saveToGallery(context: Context, imageBytes: ByteArray): Result = withContext(Dispatchers.IO) {
        try {
            val displayName = "ContentApp_${System.currentTimeMillis()}.jpg"
            val values = ContentValues().apply {
                put(MediaStore.Images.Media.DISPLAY_NAME, displayName)
                put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    // На API 29+ обязательно указывать RELATIVE_PATH — иначе изображение
                    // попадёт в корень хранилища, а не в альбом "Pictures".
                    put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/ContentApp")
                    put(MediaStore.Images.Media.IS_PENDING, 1)
                }
            }

            val resolver = context.contentResolver
            val uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
                ?: return@withContext Result.Failed("Не удалось создать запись в галерее")

            resolver.openOutputStream(uri)?.use { it.write(imageBytes) }
                ?: return@withContext Result.Failed("Не удалось открыть поток для записи")

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                values.clear()
                values.put(MediaStore.Images.Media.IS_PENDING, 0)
                resolver.update(uri, values, null, null)
            }

            Result.Success
        } catch (e: Exception) {
            Result.Failed("Ошибка сохранения: ${e.message}")
        }
    }
}
