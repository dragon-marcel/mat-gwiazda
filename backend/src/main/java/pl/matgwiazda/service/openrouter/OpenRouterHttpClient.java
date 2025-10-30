package pl.matgwiazda.service.openrouter;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import pl.matgwiazda.config.OpenRouterProperties;
import pl.matgwiazda.service.OpenRouterException;

import java.util.List;

/**
 * Encapsulates HTTP communication with OpenRouter.
 * Single responsibility: perform a single POST attempt and return raw response body or throw a domain exception.
 */
@Component
public class OpenRouterHttpClient {
    private static final Logger log = LoggerFactory.getLogger(OpenRouterHttpClient.class);
    private static final String METADATA = "metadata";
    private static final int MAX_TRUNC_LEN = 400;

    private final RestTemplate restTemplate;
    private final OpenRouterProperties properties;
    private final ObjectMapper objectMapper;

    public OpenRouterHttpClient(RestTemplateBuilder restTemplateBuilder, OpenRouterProperties properties, ObjectMapper objectMapper) {
        this.properties = properties;
        this.objectMapper = objectMapper;

        // configure request factory with timeouts using the JDK-based factory (no extra Apache dependency)
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        int timeoutMs = Math.toIntExact(Math.max(1, properties.getTimeoutMs()));
        requestFactory.setConnectTimeout(timeoutMs);
        requestFactory.setReadTimeout(timeoutMs);

        // Do not call deprecated RestTemplateBuilder.setConnectTimeout / setReadTimeout (3.4+).
        // Rely on the request factory timeouts above and build the RestTemplate normally.
        this.restTemplate = restTemplateBuilder
                .requestFactory(() -> requestFactory)
                .build();
    }

    public String post(String requestBody) {
        if (!StringUtils.hasText(properties.getApiKey())) {
            throw new OpenRouterException("OpenRouter API key is not configured. Set openrouter.apiKey or use environment variable.");
        }

        final String url = properties.getEndpoint();

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setAccept(List.of(MediaType.APPLICATION_JSON));
        headers.set("User-Agent", "MatGwiazda/1.0");
        headers.setBearerAuth(properties.getApiKey());
        HttpEntity<String> entity = new HttpEntity<>(requestBody, headers);

        log.debug("Calling OpenRouter (single attempt)");
        try {
            ResponseEntity<String> resp = restTemplate.exchange(url, HttpMethod.POST, entity, String.class);
            int status = resp.getStatusCode().value();
            String body = resp.getBody();

            if (status >= 200 && status < 300) return body;

            if (status == 401) {
                String provider = extractProviderMessage(body);
                throw new OpenRouterException("OpenRouter returned 401 Unauthorized" + (provider != null ? ": " + provider : ""));
            }

            throw new OpenRouterException("OpenRouter request failed with status: " + status + ", body: " + safeTruncate(body));

        } catch (RestClientResponseException rcre) {
            int status = rcre.getStatusCode().value();
            String respBody = rcre.getResponseBodyAsString();
            if (status == 401) throw new OpenRouterException("OpenRouter returned 401 Unauthorized: " + safeTruncate(respBody));
            log.warn("RestClientResponseException while calling OpenRouter:", rcre);
            throw new OpenRouterException("OpenRouter request failed: " + rcre.getMessage(), rcre);
        } catch (RestClientException ex) {
            log.warn("Network error while calling OpenRouter:", ex);
            throw new OpenRouterException("Network error calling OpenRouter: " + ex.getMessage(), ex);
        }
    }

    private String extractProviderMessage(String body) {
        try {
            if (body == null) return null;
            JsonNode err = objectMapper.readTree(body);
            if (err.has("error")) {
                JsonNode e = err.get("error");
                if (e.has("message")) return e.get("message").asText();
                if (e.has(METADATA) && e.get(METADATA).has("raw")) {
                    return e.get(METADATA).get("raw").asText();
                }
            }
        } catch (Exception ex) {
            log.debug("Failed to parse provider error message from OpenRouter response: {}", ex.getMessage());
        }
        return null;
    }

    private String safeTruncate(String s) {
        if (s == null) return "";
        if (MAX_TRUNC_LEN <= 0) return "";
        if (s.length() <= MAX_TRUNC_LEN) return s;
        return s.substring(0, MAX_TRUNC_LEN) + "...[truncated]";
    }
}

