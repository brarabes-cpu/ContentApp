package com.example.contentapp

import android.content.ContentValues
import android.content.Context
import android.os.Build
import android.provider.MediaStore
import java.io.PrintWriter
import java.io.StringWriter

/**
 * ВРЕМЕННЫЙ инструмент отладки — не часть постоянной архитектуры приложения.
 * Перехватывает необработанные краши и сохраняет текст стектрейса в общую папку
 * "Загрузки" через MediaStore (тот же подход, что ImageSaver.kt использует для
 * галереи — scoped storage-совместимо, не требует разрешений на API 29+), чтобы
 * можно было прочитать причину краша через обычный файловый менеджер, без ADB.
 *
 * Убрать после того, как найдём и починим причину текущего краша на старте —
 * см. MainActivity.kt, где подключается.
 */
object CrashLogger {

    fun install(context: Context) {
        val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            try {
                saveCrashLog(context, throwable)
            } catch (e: Exception) {
                // Если даже сохранение лога упало — не мешаем системе показать
                // стандартный краш-диалог, просто идём дальше без лога.
            }
            defaultHandler?.uncaughtException(thread, throwable)
        }
    }

    private fun saveCrashLog(context: Context, throwable: Throwable) {
        val stringWriter = StringWriter()
        throwable.printStackTrace(PrintWriter(stringWriter))
        val text = "Краш ContentApp, ${java.util.Date()}\n\n$stringWriter"

        val displayName = "ContentApp_crash_${System.currentTimeMillis()}.txt"
        val values = ContentValues().apply {
            put(MediaStore.Downloads.DISPLAY_NAME, displayName)
            put(MediaStore.Downloads.MIME_TYPE, "text/plain")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                put(MediaStore.Downloads.IS_PENDING, 1)
            }
        }

        val resolver = context.contentResolver
        val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values) ?: return
        resolver.openOutputStream(uri)?.use { it.write(text.toByteArray()) }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            values.clear()
            values.put(MediaStore.Downloads.IS_PENDING, 0)
            resolver.update(uri, values, null, null)
        }
    }
}
