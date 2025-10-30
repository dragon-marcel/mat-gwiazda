package pl.matgwiazda.service.openrouter;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import pl.matgwiazda.service.AiTaskResult;
import pl.matgwiazda.service.OpenRouterException;
import pl.matgwiazda.dto.openrouter.response.ChatCompletionResponse;
import pl.matgwiazda.dto.openrouter.response.Choice;
import pl.matgwiazda.dto.openrouter.response.Message;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Component
public class OpenRouterResponseParser {
    private static final Logger log = LoggerFactory.getLogger(OpenRouterResponseParser.class);

    private final ObjectMapper objectMapper;

    public OpenRouterResponseParser(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public AiTaskResult parseAiTask(String rawResponse) {
        if (rawResponse == null) throw new OpenRouterException("Empty response from OpenRouter");

        JsonNode contentNode = null;

        // Try DTO deserialization first
        try {
            ChatCompletionResponse respDto = objectMapper.readValue(rawResponse, ChatCompletionResponse.class);
            if (respDto != null && respDto.choices() != null && !respDto.choices().isEmpty()) {
                Choice first = respDto.choices().get(0);
                if (first != null) {
                    Message msg = first.message();
                    if (msg != null && msg.content() != null && !msg.content().isNull()) {
                        contentNode = msg.content();
                    } else if (first.text() != null) {
                        contentNode = objectMapper.getNodeFactory().textNode(first.text());
                    }
                }
            }
        } catch (Exception e) {
            log.debug("DTO parse failed, falling back to JsonNode parsing: {}", e.getMessage());
        }

        // Fallback
        if (contentNode == null) {
            try {
                JsonNode root = objectMapper.readTree(rawResponse);
                if (root.has("choices") && root.get("choices").isArray() && !root.get("choices").isEmpty()) {
                    JsonNode first = root.get("choices").get(0);
                    if (first.has("message") && first.get("message").has("content")) {
                        contentNode = first.get("message").get("content");
                    } else if (first.has("text")) {
                        contentNode = first.get("text");
                    }
                }
            } catch (IOException ex) {
                throw new OpenRouterException("Failed to parse OpenRouter response as JSON: " + ex.getMessage(), ex);
            }
        }

        if (contentNode == null || contentNode.isNull()) throw new OpenRouterException("AI response did not contain a message content");

        String content = extractContentText(contentNode);

        // now parse content into expected task JSON
        try {
            JsonNode resultJson = objectMapper.readTree(content);
            validateTaskJson(resultJson);

            String prompt = resultJson.get("prompt").asText();
            JsonNode optionsNode = resultJson.has("choices") ? resultJson.get("choices") : resultJson.get("options");
            List<String> options = new ArrayList<>();
            for (JsonNode n : optionsNode) options.add(n.asText());
            int correctIndex = resultJson.get("correctIndex").asInt(-1);
            String explanation = resultJson.has("explanation") ? resultJson.get("explanation").asText(null) : null;
            return new AiTaskResult(prompt, options, correctIndex, explanation);
        } catch (IOException ex) {
            String cleaned = extractJsonSubstring(content);
            try {
                JsonNode resultJson = objectMapper.readTree(cleaned);
                validateTaskJson(resultJson);
                String prompt = resultJson.get("prompt").asText();
                JsonNode optionsNode = resultJson.has("choices") ? resultJson.get("choices") : resultJson.get("options");
                List<String> options = new ArrayList<>();
                for (JsonNode n : optionsNode) options.add(n.asText());
                int correctIndex = resultJson.get("correctIndex").asInt(-1);
                String explanation = resultJson.has("explanation") ? resultJson.get("explanation").asText(null) : null;
                return new AiTaskResult(prompt, options, correctIndex, explanation);
            } catch (IOException ex2) {
                throw new OpenRouterException("Failed to parse AI output as JSON: " + ex2.getMessage(), ex2);
            }
        }
    }

    private void validateTaskJson(JsonNode resultJson) {
        if (!resultJson.has("prompt") || !resultJson.has("correctIndex") || !(resultJson.has("choices") || resultJson.has("options"))) {
            throw new OpenRouterException("AI output JSON missing required fields (prompt, correctIndex and choices/options)");
        }
        JsonNode optionsNode = resultJson.has("choices") ? resultJson.get("choices") : resultJson.get("options");
        if (!optionsNode.isArray() || optionsNode.size() < 2) {
            throw new OpenRouterException("AI output `choices/options` must be a JSON array with at least 2 elements");
        }
        int correctIndex = resultJson.get("correctIndex").asInt(-1);
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
            if (first.isTextual()) return first.asText();
            if (first.has("text")) return first.get("text").asText();
            if (first.has("content")) return first.get("content").asText();
            return first.toString();
        }
        if (contentNode.isObject()) {
            if (contentNode.has("text")) return contentNode.get("text").asText();
            if (contentNode.has("content")) return contentNode.get("content").asText();
        }
        return contentNode.toString();
    }
}

