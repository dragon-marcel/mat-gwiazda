package pl.matgwiazda.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Configuration properties for OpenRouter integration.
 */
@Component
@ConfigurationProperties(prefix = "openrouter")
public class OpenRouterProperties {

    private String apiKey;
    private String endpoint = "https://openrouter.ai/api/v1/chat/completions";
    private String defaultModel = "google/gemini-2.0-flash-exp:free"; // user requested free model
    private int timeoutMs = 30000;
    private int maxRetries = 1;
    private int backoffBaseMs = 500;

    public String getApiKey() {
        return apiKey;
    }

    public void setApiKey(String apiKey) {
        this.apiKey = apiKey;
    }

    public String getEndpoint() {
        return endpoint;
    }

    public void setEndpoint(String endpoint) {
        this.endpoint = endpoint;
    }

    public String getDefaultModel() {
        return defaultModel;
    }

    public void setDefaultModel(String defaultModel) {
        this.defaultModel = defaultModel;
    }

    public int getTimeoutMs() {
        return timeoutMs;
    }

    public void setTimeoutMs(int timeoutMs) {
        this.timeoutMs = timeoutMs;
    }

    public int getMaxRetries() {
        return maxRetries;
    }

    public void setMaxRetries(int maxRetries) {
        this.maxRetries = maxRetries;
    }

    public int getBackoffBaseMs() {
        return backoffBaseMs;
    }

    public void setBackoffBaseMs(int backoffBaseMs) {
        this.backoffBaseMs = backoffBaseMs;
    }
}
