package com.example.contentapp.content

/**
 * Единственная модель генерации в v1 (без уровней доступа и оплаты).
 * Gemini 2.5 Flash Image ("Nano Banana") — бесплатный тариф без карты (точный дневной
 * лимит уточнить в консоли: источники расходятся, ориентировочно 50–500 запросов/день).
 */
class GeminiFlashImageGenerationService : BaseGeminiImageGenerationService("gemini-2.5-flash-image")
