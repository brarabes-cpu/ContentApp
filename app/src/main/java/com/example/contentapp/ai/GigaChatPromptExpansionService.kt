package com.example.contentapp.ai

import com.example.contentapp.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.util.concurrent.TimeUnit

/**
 * Разворачивает короткую фразу через GigaChat API (Сбер) — в отличие от Gemini,
 * работает из России без VPN и без зарубежной карты (см. docs/DECISIONS.md,
 * раздел "Переход на российский стек ИИ").
 *
 * OAuth-токен получается и кешируется в GigaChatAuth.kt — общий для P1 и P4
 * (см. content/GigaChatImageGenerationService.kt), один и тот же ключ и токен на оба.
 *
 * Ключ берётся из local.properties -> BuildConfig.GIGACHAT_AUTH_KEY, в код не зашивается.
 * Получить: https://developers.sber.ru/studio -> API GigaChat -> получить ключ авторизации.
 */
class GigaChatPromptExpansionService : PromptExpansionService {

    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    private val chatUrl = "https://gigachat.devices.sberbank.ru/api/v1/chat/completions"
    private val filesUrl = "https://gigachat.devices.sberbank.ru/api/v1/files"

    override suspend fun expandPrompt(shortPrompt: String, photoBytes: ByteArray?): PromptExpansionResult {
        val authKey = BuildConfig.GIGACHAT_AUTH_KEY
        if (authKey.isBlank()) {
            return PromptExpansionResult.Failed(
                "Не задан GIGACHAT_AUTH_KEY в local.properties (см. local.properties.example)"
            )
        }

        return withContext(Dispatchers.IO) {
            try {
                val accessToken = GigaChatAuth.ensureAccessToken(authKey)
                    ?: return@withContext PromptExpansionResult.Failed("Не удалось получить токен GigaChat (проверь GIGACHAT_AUTH_KEY и сертификат Минцифры)")

                // Фото грузится отдельным запросом на /files -> получаем file_id, ссылаемся
                // на него в сообщении (мультимодальный ввод GigaChat). Если загрузка не
                // удалась — не блокируем весь запрос, просто уходим без фото.
                val fileId = photoBytes?.let { uploadPhoto(accessToken, it) }

                val messageJson = JSONObject().apply {
                    put("role", "user")
                    put("content", buildInstruction(shortPrompt))
                    if (fileId != null) put("attachments", JSONArray().put(fileId))
                }
                val requestJson = JSONObject().apply {
                    put("model", "GigaChat")
                    put("messages", JSONArray().put(messageJson))
                }

                val request = Request.Builder()
                    .url(chatUrl)
                    .addHeader("Authorization", "Bearer $accessToken")
                    .post(requestJson.toString().toRequestBody("application/json".toMediaType()))
                    .build()

                client.newCall(request).execute().use { response ->
                    val bodyText = response.body?.string().orEmpty()
                    if (!response.isSuccessful) {
                        return@withContext PromptExpansionResult.Failed(
                            "GigaChat API вернул ошибку ${response.code}: ${extractErrorMessage(bodyText)}"
                        )
                    }
                    val expanded = extractText(bodyText)
                        ?: return@withContext PromptExpansionResult.Failed(
                            "GigaChat API не вернул текст в ответе"
                        )
                    PromptExpansionResult.Success(expanded.trim())
                }
            } catch (e: IOException) {
                PromptExpansionResult.Failed("Сетевая ошибка: ${e.message}")
            }
        }
    }

    /** Загружает фото через /files, возвращает file_id для последующей ссылки в сообщении. */
    private fun uploadPhoto(accessToken: String, photoBytes: ByteArray): String? {
        val multipart = MultipartBody.Builder()
            .setType(MultipartBody.FORM)
            .addFormDataPart("purpose", "general")
            .addFormDataPart(
                "file", "photo.jpg",
                photoBytes.toRequestBody("image/jpeg".toMediaType())
            )
            .build()

        val request = Request.Builder()
            .url(filesUrl)
            .addHeader("Authorization", "Bearer $accessToken")
            .post(multipart)
            .build()

        return try {
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return null
                val json = JSONObject(response.body?.string().orEmpty())
                if (json.has("id")) json.getString("id") else null
            }
        } catch (e: IOException) {
            null
        }
    }

    private fun buildInstruction(shortPrompt: String): String = """
        Разверни короткую фразу пользователя в детализированный промт для генерации
        изображения: добавь конкретику по объекту, окружению, освещению, стилю.
        Ответь только текстом развёрнутого промта, без пояснений и без кавычек.
        Ответь строго на русском языке, даже если исходная фраза написана на
        другом языке или содержит иностранные слова.
        Фраза пользователя: "$shortPrompt"
    """.trimIndent()

    private fun extractText(responseBody: String): String? = try {
        JSONObject(responseBody)
            .getJSONArray("choices")
            .getJSONObject(0)
            .getJSONObject("message")
            .getString("content")
    } catch (e: Exception) {
        null
    }

    private fun extractErrorMessage(responseBody: String): String = try {
        JSONObject(responseBody).optString("message", responseBody.take(200))
    } catch (e: Exception) {
        responseBody.take(200)
    }
}
