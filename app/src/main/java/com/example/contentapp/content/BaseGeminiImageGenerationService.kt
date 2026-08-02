package com.example.contentapp.content

import android.util.Base64
import com.example.contentapp.BuildConfig
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

/**
 * Общая логика вызова Gemini image-generation API (`generateContent`, запрос/ответ,
 * разбор base64-картинки из ответа). Конкретная модель задаётся наследником
 * (см. GeminiFlashImageGenerationService.kt — единственная реализация в v1).
 *
 * Использует тот же GEMINI_API_KEY из local.properties, что и
 * GeminiPromptExpansionService (один ключ на оба вида запросов к Gemini).
 */
abstract class BaseGeminiImageGenerationService(private val modelId: String) : ImageGenerationService {

    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)   // генерация картинки дольше, чем текстовый ответ
        .build()

    private val endpoint =
        "https://generativelanguage.googleapis.com/v1beta/models/$modelId:generateContent"

    override suspend fun generateImage(prompt: String): ImageGenerationResult {
        val apiKey = BuildConfig.GEMINI_API_KEY
        if (apiKey.isBlank()) {
            return ImageGenerationResult.Failed(
                "Не задан GEMINI_API_KEY в local.properties (см. local.properties.example)"
            )
        }

        return withContext(Dispatchers.IO) {
            try {
                val requestJson = buildRequestBody(prompt)
                val request = Request.Builder()
                    .url("$endpoint?key=$apiKey")
                    .post(requestJson.toString().toRequestBody("application/json".toMediaType()))
                    .build()

                client.newCall(request).execute().use { response ->
                    val bodyText = response.body?.string().orEmpty()
                    if (!response.isSuccessful) {
                        return@withContext ImageGenerationResult.Failed(
                            "Gemini API ($modelId) вернул ошибку ${response.code}: ${extractErrorMessage(bodyText)}"
                        )
                    }
                    val imageBase64 = extractImageBase64(bodyText)
                        ?: return@withContext ImageGenerationResult.Failed(
                            "Gemini API ($modelId) не вернул изображение в ответе"
                        )
                    ImageGenerationResult.Success(Base64.decode(imageBase64, Base64.DEFAULT))
                }
            } catch (e: IOException) {
                ImageGenerationResult.Failed("Сетевая ошибка ($modelId): ${e.message}")
            }
        }
    }

    private fun buildRequestBody(prompt: String): JSONObject {
        val parts = JSONArray().put(JSONObject().put("text", prompt))
        val content = JSONObject().put("parts", parts)
        return JSONObject().put("contents", JSONArray().put(content))
    }

    /** Ищет первую часть ответа с inline_data (картинкой) — текстовые части (если есть) пропускает. */
    private fun extractImageBase64(responseBody: String): String? = try {
        val parts = JSONObject(responseBody)
            .getJSONArray("candidates")
            .getJSONObject(0)
            .getJSONObject("content")
            .getJSONArray("parts")
        var result: String? = null
        for (i in 0 until parts.length()) {
            val part = parts.getJSONObject(i)
            if (part.has("inline_data")) {
                result = part.getJSONObject("inline_data").getString("data")
                break
            }
        }
        result
    } catch (e: Exception) {
        null
    }

    private fun extractErrorMessage(responseBody: String): String = try {
        JSONObject(responseBody).getJSONObject("error").getString("message")
    } catch (e: Exception) {
        responseBody.take(200)
    }
}
