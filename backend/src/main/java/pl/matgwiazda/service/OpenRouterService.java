package pl.matgwiazda.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import pl.matgwiazda.config.OpenRouterProperties;
import pl.matgwiazda.dto.openrouter.ChatCompletionRequest;
import pl.matgwiazda.dto.openrouter.ChatMessage;
import pl.matgwiazda.service.openrouter.OpenRouterHttpClient;
import pl.matgwiazda.service.openrouter.OpenRouterResponseParser;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

/**
 * Orchestrates building requests for OpenRouter and parsing results.
 * Single responsibility: prepare prompt and coordinate client + parser.
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

        if (!StringUtils.hasText(this.properties.getApiKey())) {
            log.warn("OpenRouter API key is not configured (openrouter.apiKey is empty). Service will fail on calls that require the key.");
        }
    }

    public AiTaskResult generateTaskFromSeed(String seed, short level) {
        if (!StringUtils.hasText(seed)) throw new OpenRouterException("seed must not be empty");
        if (level < 1 || level > 8) throw new OpenRouterException("level must be in range 1..8");

        String systemPrompt = "You are a math problem generator for primary school students. " +
                "Given the seed produce a JSON object with fields: `prompt` (a short question), `options` (array of 4 strings), `correctIndex` (0-based integer), `explanation` (short text). Only output valid JSON in Polish.";

        String userPrompt = String.format("seed: %s%nProduce the JSON exactly without extra commentary.", seed.trim());

        try {
            log.debug("Preparing OpenRouter request: model={} endpoint={}", properties.getDefaultModel(), properties.getEndpoint());

            // Load optional response_format schema if present
            JsonNode responseFormat = null;
            try (InputStream schemaStream = getClass().getResourceAsStream("/openrouter/math_prompt_v1.json")) {
                if (schemaStream != null) responseFormat = objectMapper.readTree(schemaStream);
            } catch (IOException io) {
                log.debug("No response_format schema found on classpath or failed to read it: {}", io.getMessage());
            }

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
            return responseParser.parseAiTask(rawResponse);

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
}
