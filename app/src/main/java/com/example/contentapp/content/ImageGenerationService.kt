package com.example.contentapp.content

/**
 * Генерирует изображение по финальному промту (после S1.B3/S1.B4). Реализует
 * получение контента из NEXT-STEPS.md — контент генерируется ИИ "на лету",
 * а не подбирается из готовой библиотеки (см. DECISIONS.md).
 *
 * Общий интерфейс — AppNavigation.kt работает только через него, не зная,
 * какая именно модель генерирует изображение (по аналогии с PaymentProvider
 * и PromptExpansionService).
 */
interface ImageGenerationService {
    /** @param prompt финальный текст — из S1.F1 (S1.B3) или S1.F2 (S1.B4) */
    suspend fun generateImage(prompt: String): ImageGenerationResult
}
