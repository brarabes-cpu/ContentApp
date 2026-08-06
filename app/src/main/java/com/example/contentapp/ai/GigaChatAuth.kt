package com.example.contentapp.ai

import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.IOException
import java.util.UUID

/**
 * Общая OAuth2-логика GigaChat API (Сбер) — единственная точка получения access_token,
 * используется и P1 (GigaChatPromptExpansionService.kt), и P4
 * (GigaChatImageGenerationService.kt), чтобы не дублировать обмен ключа на токен и не
 * плодить независимые кеши/запросы токена для одного и того же ключа.
 *
 * Авторизация — OAuth2 client credentials: "ключ авторизации" (готовый Base64 из
 * личного кабинета developers.sber.ru) меняется на access_token, живущий 30 минут;
 * токен кешируется в памяти процесса (object = singleton) и обновляется по истечении.
 *
 * ВАЖНО: GigaChat API требует доверенный корневой сертификат НУЦ Минцифры — без него
 * TLS-соединение обрывается. Настроено через res/xml/network_security_config.xml
 * (см. AndroidManifest.xml) — но сами файлы сертификатов (res/raw/russian_trusted_*.crt)
 * нужно скачать отдельно, см. docs/NEXT-STEPS.md, шаг про сертификаты.
 */
object GigaChatAuth {

    private val client = OkHttpClient()
    private const val oauthUrl = "https://ngw.devices.sberbank.ru:9443/api/v2/oauth"

    private var cachedToken: String? = null
    private var cachedTokenExpiresAtMillis: Long = 0L

    /** Возвращает валидный access_token, запрашивая новый, если старый истёк или отсутствует. */
    fun ensureAccessToken(authKey: String): String? {
        val now = System.currentTimeMillis()
        // Запрашиваем заново на минуту раньше формального истечения — запас на сетевую задержку.
        if (cachedToken != null && now < cachedTokenExpiresAtMillis - 60_000) {
            return cachedToken
        }

        val body = "scope=GIGACHAT_API_PERS".toRequestBody("application/x-www-form-urlencoded".toMediaType())
        val request = Request.Builder()
            .url(oauthUrl)
            .addHeader("Authorization", "Basic $authKey")
            .addHeader("RqUID", UUID.randomUUID().toString())
            .addHeader("Accept", "application/json")
            .post(body)
            .build()

        return try {
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return null
                val json = JSONObject(response.body?.string().orEmpty())
                cachedToken = json.getString("access_token")
                cachedTokenExpiresAtMillis = json.getLong("expires_at")
                cachedToken
            }
        } catch (e: IOException) {
            null
        }
    }
}
