package com.example.contentapp.content

import com.example.contentapp.BuildConfig
import com.example.contentapp.ai.GigaChatAuth
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.util.concurrent.TimeUnit
import java.util.regex.Pattern

/**
 * Генерация изображения через встроенную функцию text2image GigaChat API (Сбер) —
 * тот же провайдер, что и P1 (см. ai/GigaChatPromptExpansionService.kt), тот же ключ,
 * тот же уже настроенный сертификат НУЦ Минцифры, общий OAuth-токен (GigaChatAuth.kt).
 *
 * Изначально пробовали отдельные провайдеры (Kandinsky/FusionBrain — оказался
 * практически недоступен, затем YandexART — требует привязку карты к Yandex Cloud
 * даже для бесплатного лимита). Встроенная функция GigaChat не требует ни карты, ни
 * второго набора ключей — прямой компромисс: качество и стиль результата проще, чем у
 * специализированного API генерации изображений, и разрешение жёстко 1024×1024
 * (см. docs/DECISIONS.md, раздел "Консолидация P1+P4 на GigaChat").
 *
 * Механика: обычный запрос в /chat/completions с "function_call": "auto" и промтом,
 * содержащим слово "нарисуй" (без него функция может не вызваться) — модель сама решает
 * вызвать text2image. Ответ содержит HTML-подобный content с <img src="file_id" .../>;
 * сама картинка скачивается отдельным запросом на /files/{file_id}/content.
 */
class GigaChatImageGenerationService : ImageGenerationService {

    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS) // генерация картинки медленнее обычного ответа
        .build()

    private val chatUrl = "https://gigachat.devices.sberbank.ru/api/v1/chat/completions"
    private val filesUrlBase = "https://gigachat.devices.sberbank.ru/api/v1/files"

    private val imgSrcPattern = Pattern.compile("""<img\s+src=["']([^"']+)["']""")

    override suspend fun generateImage(prompt: String): ImageGenerationResult {
        val authKey = BuildConfig.GIGACHAT_AUTH_KEY
        if (authKey.isBlank()) {
            return ImageGenerationResult.Failed(
                "Не задан GIGACHAT_AUTH_KEY в local.properties (см. local.properties.example)"
            )
        }

        return withContext(Dispatchers.IO) {
            try {
                val accessToken = GigaChatAuth.ensureAccessToken(authKey)
                    ?: return@withContext ImageGenerationResult.Failed("Не удалось получить токен GigaChat (проверь GIGACHAT_AUTH_KEY и сертификат Минцифры)")

                val requestJson = JSONObject().apply {
                    put("model", "GigaChat")
                    put("function_call", "auto")
                    put(
                        "messages",
                        JSONArray().put(
                            JSONObject().apply {
                                put("role", "user")
                                // "нарисуй" обязательно в тексте — иначе модель может не
                                // вызвать text2image, а просто ответить текстом.
                                put("content", "Нарисуй: $prompt")
                            }
                        )
                    )
                }

                val request = Request.Builder()
                    .url(chatUrl)
                    .addHeader("Authorization", "Bearer $accessToken")
                    .post(requestJson.toString().toRequestBody("application/json".toMediaType()))
                    .build()

                val fileId = client.newCall(request).execute().use { response ->
                    val bodyText = response.body?.string().orEmpty()
                    if (!response.isSuccessful) {
                        return@withContext ImageGenerationResult.Failed(
                            "GigaChat API вернул ошибку ${response.code}: ${extractErrorMessage(bodyText)}"
                        )
                    }
                    extractFileId(bodyText)
                } ?: return@withContext ImageGenerationResult.Failed(
                    "GigaChat не сгенерировал изображение (ответ не содержал картинку — возможно, промт не распознан как запрос на рисование)"
                )

                downloadImage(accessToken, fileId)
            } catch (e: IOException) {
                ImageGenerationResult.Failed("Сетевая ошибка: ${e.message}")
            }
        }
    }

    /** Достаёт id картинки из content вида `Готово! <img src="fileid" fuse="true"/>`. */
    private fun extractFileId(responseBody: String): String? {
        val content = try {
            JSONObject(responseBody)
                .getJSONArray("choices")
                .getJSONObject(0)
                .getJSONObject("message")
                .getString("content")
        } catch (e: Exception) {
            return null
        }
        val matcher = imgSrcPattern.matcher(content)
        return if (matcher.find()) matcher.group(1) else null
    }

    private fun downloadImage(accessToken: String, fileId: String): ImageGenerationResult {
        val request = Request.Builder()
            .url("$filesUrlBase/$fileId/content")
            .addHeader("Authorization", "Bearer $accessToken")
            .get()
            .build()

        return try {
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    return ImageGenerationResult.Failed("Не удалось скачать изображение: ошибка ${response.code}")
                }
                val bytes = response.body?.bytes()
                if (bytes == null || bytes.isEmpty()) {
                    ImageGenerationResult.Failed("GigaChat вернул пустой файл изображения")
                } else {
                    ImageGenerationResult.Success(bytes)
                }
            }
        } catch (e: IOException) {
            ImageGenerationResult.Failed("Сетевая ошибка при скачивании изображения: ${e.message}")
        }
    }

    private fun extractErrorMessage(responseBody: String): String = try {
        JSONObject(responseBody).optString("message", responseBody.take(200))
    } catch (e: Exception) {
        responseBody.take(200)
    }
}
