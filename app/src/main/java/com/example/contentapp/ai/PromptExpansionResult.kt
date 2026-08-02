package com.example.contentapp.ai

/**
 * Результат вызова PromptExpansionService.expandPrompt() — экраны решают, что
 * делать дальше, сервис только сообщает, что произошло.
 */
sealed class PromptExpansionResult {
    data class Success(val expandedPrompt: String) : PromptExpansionResult()
    data class Failed(val reason: String) : PromptExpansionResult()
}
