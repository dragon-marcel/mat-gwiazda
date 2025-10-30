package pl.matgwiazda.dto.openrouter.response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * Represents a single choice in the chat/completions response.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record Choice(Message message, String text) {
}
