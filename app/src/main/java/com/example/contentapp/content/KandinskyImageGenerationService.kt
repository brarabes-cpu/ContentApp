package com.example.contentapp.content

import android.util.Base64
import com.example.contentapp.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
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
 * Генерация изображения через Kandinsky (FusionBrain API, Сбер) — в отличие от Gemini,
 * работает из России без VPN и без зарубежной карты, и понимает промт на русском без
 * перевода (см. docs/DECISIONS.md, раздел "Переход на российский стек ИИ").
 *
 * В отличие от Gemini (один синхронный запрос), FusionBrain API асинхронный:
 * pipeline/run запускает генерацию и возвращает uuid задачи, дальше опрашиваем
 * pipeline/status/{uuid}, пока не готово (статус DONE/FAIL) — см. pollResult().
 *
 * Ключи берутся из local.properties -> BuildConfig.FUSIONBRAIN_API_KEY / FUSIONBRAIN_SECRET_KEY,
 * в код не зашиваются. Получить: https://fusionbrain.ai/keys/
 */
class KandinskyImageGenerationService : ImageGenerationService {

    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    private val baseUrl = "https://api-key.fusionbrain.ai/key/api/v1"

    // Pipeline (модель) меняется редко — кешируем в памяти процесса, чтобы не запрашивать
    // на каждую генерацию.
    private var cachedPipelineId: String? = null

    override suspend fun generateImage(prompt: String): ImageGenerationResult {
        val apiKey = BuildConfig.FUSIONBRAIN_API_KEY
        val secretKey = BuildConfig.FUSIONBRAIN_SECRET_KEY
        if (apiKey.isBlank() || secretKey.isBlank()) {
            return ImageGenerationResult.Failed(
                "Не заданы FUSIONBRAIN_API_KEY/FUSIONBRAIN_SECRET_KEY в local.properties"
            )
        }

        return withContext(Dispatchers.IO) {
            try {
                val pipelineId = ensurePipelineId(apiKey, secretKey)
                    ?: return@withContext ImageGenerationResult.Failed("Не удалось получить pipeline Kandinsky")

                val uuid = runGeneration(apiKey, secretKey, pipelineId, prompt)
                    ?: return@withContext ImageGenerationResult.Failed("Kandinsky не принял запрос на генерацию")

                pollResult(apiKey, secretKey, uuid)
            } catch (e: IOException) {
                ImageGenerationResult.Failed("Сетевая ошибка: ${e.message}")
            }
        }
    }

    private fun authRequest(url: String, apiKey: String, secretKey: String) =
        Request.Builder()
            .url(url)
            .addHeader("X-Key", "Key $apiKey")
            .addHeader("X-Secret", "Secret $secretKey")

    private fun ensurePipelineId(apiKey: String, secretKey: String): String? {
        cachedPipelineId?.let { return it }
        val request = authRequest("$baseUrl/pipelines", apiKey, secretKey).get().build()
        return try {
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return null
                val pipelines = JSONArray(response.body?.string().orEmpty())
                if (pipelines.length() == 0) return null
                cachedPipelineId = pipelines.getJSONObject(0).getString("id")
                cachedPipelineId
            }
        } catch (e: IOException) {
            null
        }
    }

    private fun runGeneration(apiKey: String, secretKey: String, pipelineId: String, prompt: String): String? {
        val params = JSONObject().apply {
            put("type", "GENERATE")
            put("numImages", 1)
            put("width", 1024)
            put("height", 1024)
            put("generateParams", JSONObject().put("query", prompt))
        }

        val multipart = MultipartBody.Builder()
            .setType(MultipartBody.FORM)
            .addFormDataPart("pipeline_id", pipelineId)
            .addFormDataPart(
                "params", "params.json",
                params.toString().toRequestBody("application/json".toMediaType())
            )
            .build()

        val request = authRequest("$baseUrl/pipeline/run", apiKey, secretKey).post(multipart).build()
        return try {
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return null
                val uuid = JSONObject(response.body?.string().orEmpty()).optString("uuid")
                uuid.ifBlank { null }
            }
        } catch (e: IOException) {
            null
        }
    }

    /** Опрашивает статус задачи, пока не готово — до ~60 секунд (30 попыток по 2 секунды). */
    private suspend fun pollResult(apiKey: String, secretKey: String, uuid: String): ImageGenerationResult {
        repeat(30) {
            delay(2000)
            val request = authRequest("$baseUrl/pipeline/status/$uuid", apiKey, secretKey).get().build()
            try {
                client.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) {
                        return ImageGenerationResult.Failed("Kandinsky API вернул ошибку ${response.code}")
                    }
                    val json = JSONObject(response.body?.string().orEmpty())
                    when (json.optString("status")) {
                        "DONE" -> {
                            val files = json.getJSONObject("result").getJSONArray("files")
                            if (files.length() == 0) {
                                return ImageGenerationResult.Failed("Kandinsky не вернул изображение")
                            }
                            val imageBase64 = files.getString(0)
                            return ImageGenerationResult.Success(Base64.decode(imageBase64, Base64.DEFAULT))
                        }
                        "FAIL" -> return ImageGenerationResult.Failed(
                            "Генерация не удалась (промт мог быть отклонён цензурой Kandinsky)"
                        )
                        else -> { /* INITIAL / PROCESSING — ещё в очереди, продолжаем опрос */ }
                    }
                }
            } catch (e: IOException) {
                return ImageGenerationResult.Failed("Сетевая ошибка при опросе статуса: ${e.message}")
            }
        }
        return ImageGenerationResult.Failed("Превышено время ожидания генерации (60 сек)")
    }
}
