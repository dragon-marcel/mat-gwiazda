package pl.matgwiazda.dto.openrouter.response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;

/**
 * Minimal POJO for deserializing OpenRouter chat/completions responses.
 * We keep fields small and focused to avoid coupling to provider extras.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record ChatCompletionResponse(List<Choice> choices) {
}
