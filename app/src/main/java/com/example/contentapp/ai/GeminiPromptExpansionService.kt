package com.example.contentapp.ai

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
 * Разворачивает короткую фразу через Gemini API (модель gemini-2.5-flash — бесплатный
 * тариф, без карты, поддерживает текст + фото на входе).
 *
 * Ключ берётся из local.properties -> BuildConfig.GEMINI_API_KEY (см. app/build.gradle.kts),
 * в код не зашивается. Получить свой ключ: https://aistudio.google.com/apikey
 */
class GeminiPromptExpansionService : PromptExpansionService {

    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    private val endpoint =
        "https://generativelanguage.googleapis.com/v1beta/models/gemini-3-flash-preview:generateContent"

    override suspend fun expandPrompt(shortPrompt: String, photoBytes: ByteArray?): PromptExpansionResult {
        val apiKey = BuildConfig.GEMINI_API_KEY
        if (apiKey.isBlank()) {
            return PromptExpansionResult.Failed(
                "Не задан GEMINI_API_KEY в local.properties (см. local.properties.example)"
            )
        }

        return withContext(Dispatchers.IO) {
            try {
                val requestJson = buildRequestBody(shortPrompt, photoBytes)
                val request = Request.Builder()
                    .url("$endpoint?key=$apiKey")
                    .post(requestJson.toString().toRequestBody("application/json".toMediaType()))
                    .build()

                client.newCall(request).execute().use { response ->
                    val bodyText = response.body?.string().orEmpty()
                    if (!response.isSuccessful) {
                        return@withContext PromptExpansionResult.Failed(
                            "Gemini API вернул ошибку ${response.code}: ${extractErrorMessage(bodyText)}"
                        )
                    }
                    val expanded = extractText(bodyText)
                        ?: return@withContext PromptExpansionResult.Failed(
                            "Gemini API не вернул текст в ответе"
                        )
                    PromptExpansionResult.Success(expanded.trim())
                }
            } catch (e: IOException) {
                PromptExpansionResult.Failed("Сетевая ошибка: ${e.message}")
            }
        }
    }

    private fun buildRequestBody(shortPrompt: String, photoBytes: ByteArray?): JSONObject {
        // TODO: инструкцию стоит доработать под конкретную тематику контента, когда она появится
        val instruction = """
            Разверни короткую фразу пользователя в детализированный промт для генерации
            изображения: добавь конкретику по объекту, окружению, освещению, стилю.
            Ответь только текстом развёрнутого промта, без пояснений и без кавычек.
            Фраза пользователя: "$shortPrompt"
        """.trimIndent()

        val parts = JSONArray().apply {
            put(JSONObject().put("text", instruction))
            if (photoBytes != null) {
                put(
                    JSONObject().put(
                        "inline_data",
                        JSONObject()
                            .put("mime_type", "image/jpeg")
                            .put("data", Base64.encodeToString(photoBytes, Base64.NO_WRAP))
                    )
                )
            }
        }

        val content = JSONObject().put("parts", parts)
        return JSONObject().put("contents", JSONArray().put(content))
    }

    private fun extractText(responseBody: String): String? = try {
        JSONObject(responseBody)
            .getJSONArray("candidates")
            .getJSONObject(0)
            .getJSONObject("content")
            .getJSONArray("parts")
            .getJSONObject(0)
            .getString("text")
    } catch (e: Exception) {
        null
    }

    private fun extractErrorMessage(responseBody: String): String = try {
        JSONObject(responseBody).getJSONObject("error").getString("message")
    } catch (e: Exception) {
        responseBody.take(200)
    }
}
