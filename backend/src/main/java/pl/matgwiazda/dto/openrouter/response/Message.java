package pl.matgwiazda.dto.openrouter.response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.JsonNode;

/**
 * Represents the `message` object inside a choice; content can be various shapes so we keep it as JsonNode.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record Message(JsonNode content) {
}
