package pl.matgwiazda.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import pl.matgwiazda.config.OpenRouterProperties;
import pl.matgwiazda.dto.openrouter.AiTaskResult;
import pl.matgwiazda.dto.openrouter.ChatCompletionRequest;
import pl.matgwiazda.dto.openrouter.ChatMessage;
import pl.matgwiazda.exception.OpenRouterException;
import pl.matgwiazda.service.openrouter.OpenRouterHttpClient;
import pl.matgwiazda.service.openrouter.OpenRouterResponseParser;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

/**
 * Service responsible for interacting with OpenRouter.
 *
 * Contract:
 * - Input: seed (non-empty), optional level (1..8)
 * - Output: a validated {@link AiTaskResult}
 * - Errors: throws {@link OpenRouterException} for any validation, transport or parsing errors
 */
@Service
public class OpenRouterService {

    private static final Logger log = LoggerFactory.getLogger(OpenRouterService.class);

    private final ObjectMapper objectMapper;
    private final OpenRouterProperties properties;
    private final OpenRouterHttpClient httpClient;
    private final OpenRouterResponseParser responseParser;

    public OpenRouterService(ObjectMapper objectMapper,
                             OpenRouterProperties properties,
                             OpenRouterHttpClient httpClient,
                             OpenRouterResponseParser responseParser) {
        this.objectMapper = objectMapper;
        this.properties = properties;
        this.httpClient = httpClient;
        this.responseParser = responseParser;
    }

    /**
     * Generate a new AI task based on a seed.
     * @param seed non-empty seed text used to prime the model
     * @param level difficulty 1..8
     * @return validated AiTaskResult
     * @throws OpenRouterException when configuration is missing, transport fails or response cannot be parsed
     */
    public AiTaskResult generateTaskFromSeed(String seed, short level) {
        if (!StringUtils.hasText(seed)) throw new OpenRouterException("seed must not be empty");
        if (level < 1 || level > 8) throw new OpenRouterException("level must be in range 1..8");

        // Validate configuration at call time (fail fast and loudly)
        if (!StringUtils.hasText(this.properties.getApiKey())) {
            throw new OpenRouterException("OpenRouter API key is not configured (openrouter.apiKey is empty)");
        }

        String systemPrompt = "You are a math problem generator for primary school students. " +
                "Create a new task every time a JSON object with fields: `prompt` (a short question), `options` (array of 4 strings), `correctIndex` (0-based integer), `explanation` (short text). Only output valid JSON in Polish.";

        String userPrompt = String.format("seed: %s%nProduce the JSON exactly without extra commentary.", seed.trim());

        try {
            log.debug("Preparing OpenRouter request: model={} endpoint={}", properties.getDefaultModel(), properties.getEndpoint());

            JsonNode responseFormat = loadResponseFormatIfPresent();

            List<ChatMessage> messages = List.of(
                    new ChatMessage("system", systemPrompt),
                    new ChatMessage("user", userPrompt)
            );

            ChatCompletionRequest chatRequest = new ChatCompletionRequest(
                    properties.getDefaultModel(),
                    0.0,
                    0.8,
                    400,
                    messages,
                    responseFormat
            );

            String requestBody = objectMapper.writeValueAsString(chatRequest);

            String rawResponse = httpClient.post(requestBody);

            // Delegate parsing to the response parser which returns the domain DTO
            try {
                return responseParser.parseAiTask(rawResponse);
            } catch (OpenRouterException ore) {
                // Add context about the request that failed
                throw new OpenRouterException("Failed to parse OpenRouter response: " + ore.getMessage() + "; rawResponse=" + shorten(rawResponse), ore);
            }

        } catch (IOException ex) {
            log.error("Failed preparing request for OpenRouter: {}", ex.getMessage(), ex);
            throw new OpenRouterException("Failed preparing request to OpenRouter: " + ex.getMessage(), ex);
        }
    }

    // convenience overloads
    public AiTaskResult generateTaskFromSeed(String seed) {
        return generateTaskFromSeed(seed, (short) 1);
    }

    public AiTaskResult generateTaskFromSeed(String seed, Short level) {
        short l = (level == null) ? (short) 1 : level.shortValue();
        return generateTaskFromSeed(seed, l);
    }

    private JsonNode loadResponseFormatIfPresent() throws IOException {
        try (InputStream schemaStream = getClass().getResourceAsStream("/openrouter/math_prompt_v1.json")) {
            if (schemaStream == null) return null;
            return objectMapper.readTree(schemaStream);
        }
    }

    private String shorten(String s) {
        if (s == null) return "";
        return s.length() <= 200 ? s : s.substring(0, 200) + "...[truncated]";
    }
}
