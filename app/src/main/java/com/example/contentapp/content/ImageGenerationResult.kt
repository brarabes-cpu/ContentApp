package com.example.contentapp.content

/**
 * Результат вызова ImageGenerationService.generateImage() — экраны решают, что
 * делать дальше, сервис только сообщает, что произошло.
 */
sealed class ImageGenerationResult {
    data class Success(val imageBytes: ByteArray) : ImageGenerationResult()
    data class Failed(val reason: String) : ImageGenerationResult()
}
