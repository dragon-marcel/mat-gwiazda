package pl.matgwiazda.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import pl.matgwiazda.config.OpenRouterProperties;
import pl.matgwiazda.dto.openrouter.AiTaskResult;
import pl.matgwiazda.exception.OpenRouterException;
import pl.matgwiazda.service.openrouter.OpenRouterHttpClient;
import pl.matgwiazda.service.openrouter.OpenRouterResponseParser;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class OpenRouterServiceTest {

    @Mock
    ObjectMapper objectMapper;

    @Mock
    OpenRouterProperties properties;

    @Mock
    OpenRouterHttpClient httpClient;

    @Mock
    OpenRouterResponseParser responseParser;

    @InjectMocks
    OpenRouterService openRouterService;

    @Test
    void generateTaskFromSeed_success_delegatesAndReturnsResult() {
        when(properties.getDefaultModel()).thenReturn("o-model");
        when(properties.getEndpoint()).thenReturn("/v1");
        // lenient stub for serialization (may be unused in some code paths)
        try {
            lenient().when(objectMapper.writeValueAsString(any())).thenReturn("request-body");
        } catch (JsonProcessingException e) {
            // won't happen during stubbing, but the compiler requires handling the checked exception
            throw new RuntimeException(e);
        }

        when(httpClient.post("request-body")).thenReturn("raw-response");

        AiTaskResult expected = new AiTaskResult("p", List.of("a", "b", "c", "d"), 1, "exp");
        when(responseParser.parseAiTask("raw-response")).thenReturn(expected);

        AiTaskResult result = openRouterService.generateTaskFromSeed("seed123", (short)2);

        assertThat(result).isEqualTo(expected);
    }

    @Test
    void generateTaskFromSeed_emptySeed_throws() {
        assertThrows(OpenRouterException.class, () -> openRouterService.generateTaskFromSeed("", (short)1));
        assertThrows(OpenRouterException.class, () -> openRouterService.generateTaskFromSeed(null, (short)1));
    }

    @Test
    void generateTaskFromSeed_levelOutOfRange_throws() {
        assertThrows(OpenRouterException.class, () -> openRouterService.generateTaskFromSeed("s", (short)0));
        assertThrows(OpenRouterException.class, () -> openRouterService.generateTaskFromSeed("s", (short)9));
    }

    @Test
    void generateTaskFromSeed_serializationIOException_wrappedAsOpenRouterException() {
        when(properties.getApiKey()).thenReturn("k");
        when(properties.getDefaultModel()).thenReturn("m");
        when(properties.getEndpoint()).thenReturn("e");

        // Mockito cannot always stub checked exceptions for methods that don't declare them in some contexts.
        // Create a real ObjectMapper that throws IOException from writeValueAsString and construct the service
        ObjectMapper failingMapper = new ObjectMapper() {
            @Override
            public String writeValueAsString(Object value) throws JsonProcessingException {
                throw new JsonProcessingException("boom") {};
            }
        };

        OpenRouterService svc = new OpenRouterService(failingMapper, properties, httpClient, responseParser);

        assertThrows(OpenRouterException.class, () -> svc.generateTaskFromSeed("seed", (short)1));
    }
}
