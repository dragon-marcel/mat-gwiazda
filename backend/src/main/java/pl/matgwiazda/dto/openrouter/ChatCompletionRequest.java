package pl.matgwiazda.dto.openrouter;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.List;

/**
 * DTO for the OpenRouter chat/completions request payload.
 */
public record ChatCompletionRequest(
        String model,
        Double temperature,
        Double top_p,
        Integer max_tokens,
        List<ChatMessage> messages,
        JsonNode response_format
) {
}

