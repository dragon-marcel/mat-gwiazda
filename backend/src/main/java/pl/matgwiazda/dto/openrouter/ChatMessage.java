package pl.matgwiazda.dto.openrouter;

/**
 * Simple DTO for a chat message used by OpenRouter chat/completions API.
 */
public record ChatMessage(String role, String content) {
}

