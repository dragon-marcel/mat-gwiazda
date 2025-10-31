package pl.matgwiazda.service.openrouter;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import pl.matgwiazda.dto.openrouter.AiTaskResult;
import pl.matgwiazda.exception.OpenRouterException;
import pl.matgwiazda.dto.openrouter.response.ChatCompletionResponse;
import pl.matgwiazda.dto.openrouter.response.Choice;
import pl.matgwiazda.dto.openrouter.response.Message;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Parses raw OpenRouter responses into AiTaskResult.
 */
@Component
public class OpenRouterResponseParser {
    private static final Logger log = LoggerFactory.getLogger(OpenRouterResponseParser.class);

    private static final String FIELD_CHOICES = "choices";
    private static final String FIELD_MESSAGE = "message";
    private static final String FIELD_CONTENT = "content";
    private static final String FIELD_TEXT = "text";
    private static final String FIELD_OPTIONS = "options";
    private static final String FIELD_CORRECT_INDEX = "correctIndex";
    private static final String FIELD_PROMPT = "prompt";
    private static final String FIELD_EXPLANATION = "explanation";

    private final ObjectMapper objectMapper;

    public OpenRouterResponseParser(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public AiTaskResult parseAiTask(String rawResponse) {
        if (rawResponse == null) throw new OpenRouterException("Empty response from OpenRouter");

        JsonNode contentNode = tryParseWithDto(rawResponse);
        if (contentNode == null) contentNode = tryParseGenericJson(rawResponse);
        if (contentNode == null || contentNode.isNull()) throw new OpenRouterException("AI response did not contain a message content");

        String content = extractContentText(contentNode);
        return parseTaskFromContent(content);
    }

    private JsonNode tryParseWithDto(String rawResponse) {
        try {
            ChatCompletionResponse respDto = objectMapper.readValue(rawResponse, ChatCompletionResponse.class);
            if (respDto == null || respDto.choices() == null || respDto.choices().isEmpty()) return null;
            Choice first = respDto.choices().get(0);
            if (first == null) return null;
            Message msg = first.message();
            if (msg != null && msg.content() != null && !msg.content().isNull()) return msg.content();
            if (first.text() != null) return objectMapper.getNodeFactory().textNode(first.text());
            return null;
        } catch (Exception e) {
            log.debug("DTO parse failed, falling back to JsonNode parsing: {}", e.getMessage());
            return null;
        }
    }

    private JsonNode tryParseGenericJson(String rawResponse) {
        try {
            JsonNode root = objectMapper.readTree(rawResponse);
            if (!root.has(FIELD_CHOICES) || !root.get(FIELD_CHOICES).isArray() || root.get(FIELD_CHOICES).isEmpty()) return null;
            JsonNode first = root.get(FIELD_CHOICES).get(0);
            if (first.has(FIELD_MESSAGE) && first.get(FIELD_MESSAGE).has(FIELD_CONTENT)) {
                return first.get(FIELD_MESSAGE).get(FIELD_CONTENT);
            }
            if (first.has(FIELD_TEXT)) return first.get(FIELD_TEXT);
            return null;
        } catch (IOException ex) {
            throw new OpenRouterException("Failed to parse OpenRouter response as JSON: " + ex.getMessage(), ex);
        }
    }

    private AiTaskResult parseTaskFromContent(String content) {
        if (content == null) throw new OpenRouterException("AI content is empty");

        try {
            JsonNode resultJson = objectMapper.readTree(content);
            return nodeToAiTask(resultJson);
        } catch (IOException ex) {
            String cleaned = extractJsonSubstring(content);
            try {
                JsonNode resultJson = objectMapper.readTree(cleaned);
                return nodeToAiTask(resultJson);
            } catch (IOException ex2) {
                throw new OpenRouterException("Failed to parse AI output as JSON: " + ex2.getMessage(), ex2);
            }
        }
    }

    private AiTaskResult nodeToAiTask(JsonNode resultJson) {
        validateTaskJson(resultJson);

        String prompt = resultJson.get(FIELD_PROMPT).asText();
        JsonNode optionsNode = resultJson.has(FIELD_CHOICES) ? resultJson.get(FIELD_CHOICES) : resultJson.get(FIELD_OPTIONS);
        List<String> options = new ArrayList<>();
        for (JsonNode n : optionsNode) options.add(n.asText());
        int correctIndex = resultJson.get(FIELD_CORRECT_INDEX).asInt(-1);
        String explanation = resultJson.has(FIELD_EXPLANATION) ? resultJson.get(FIELD_EXPLANATION).asText(null) : null;
        return new AiTaskResult(prompt, options, correctIndex, explanation);
    }

    private void validateTaskJson(JsonNode resultJson) {
        if (!resultJson.has(FIELD_PROMPT) || !resultJson.has(FIELD_CORRECT_INDEX) || !(resultJson.has(FIELD_CHOICES) || resultJson.has(FIELD_OPTIONS))) {
            throw new OpenRouterException("AI output JSON missing required fields (prompt, correctIndex and choices/options)");
        }
        JsonNode optionsNode = resultJson.has(FIELD_CHOICES) ? resultJson.get(FIELD_CHOICES) : resultJson.get(FIELD_OPTIONS);
        if (!optionsNode.isArray() || optionsNode.size() < 2) {
            throw new OpenRouterException("AI output `choices/options` must be a JSON array with at least 2 elements");
        }
        int correctIndex = resultJson.get(FIELD_CORRECT_INDEX).asInt(-1);
        if (correctIndex < 0 || correctIndex >= optionsNode.size()) throw new OpenRouterException("AI output `correctIndex` is out of bounds");
    }

    private String extractJsonSubstring(String content) {
        if (content == null) return "";
        int objStart = content.indexOf('{');
        int objEnd = content.lastIndexOf('}');
        if (objStart >= 0 && objEnd > objStart) return content.substring(objStart, objEnd + 1);
        int arrStart = content.indexOf('[');
        int arrEnd = content.lastIndexOf(']');
        if (arrStart >= 0 && arrEnd > arrStart) return content.substring(arrStart, arrEnd + 1);
        return content.trim();
    }

    private String extractContentText(JsonNode contentNode) {
        if (contentNode == null || contentNode.isNull()) return "";
        if (contentNode.isTextual()) return contentNode.asText();

        if (contentNode.isArray() && !contentNode.isEmpty()) {
            JsonNode first = contentNode.get(0);
            return extractTextFromNode(first);
        }

        if (contentNode.isObject()) {
            return extractTextFromNode(contentNode);
        }

        return contentNode.toString();
    }

    private String extractTextFromNode(JsonNode node) {
        if (node == null || node.isNull()) return "";
        if (node.isTextual()) return node.asText();
        if (node.has(FIELD_TEXT)) return node.get(FIELD_TEXT).asText();
        if (node.has(FIELD_CONTENT)) return node.get(FIELD_CONTENT).asText();
        return node.toString();
    }
}
