package com.example.contentapp.ai

/**
 * Разворачивает короткую фразу пользователя (S1.F1) в детализированный промт (S1.F2),
 * опционально учитывая приложенное фото. Реализует P1 из SPEC.md.
 *
 * Общий интерфейс — MainScreen.kt/AppNavigation.kt работают только через него,
 * не зная, какая именно ИИ-модель отвечает за разворачивание (по аналогии
 * с PaymentProvider в модуле payment/).
 */
interface PromptExpansionService {
    /**
     * @param shortPrompt текст из S1.F1, как ввёл пользователь
     * @param photoBytes байты приложенного фото (S1.B1), null если фото не приложено
     */
    suspend fun expandPrompt(shortPrompt: String, photoBytes: ByteArray?): PromptExpansionResult
}
