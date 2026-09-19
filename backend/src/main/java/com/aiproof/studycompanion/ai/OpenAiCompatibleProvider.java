package com.aiproof.studycompanion.ai;

import java.time.Duration;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Shared HTTP code for any provider that speaks the OpenAI
 * "chat completions" format.
 *
 * Groq  -> https://api.groq.com/openai/v1
 * HF    -> https://router.huggingface.co/v1
 *
 * Both accept the exact same request body, so we only write this once.
 */
public abstract class OpenAiCompatibleProvider implements AiProvider {

    private static final Logger log = LoggerFactory.getLogger(OpenAiCompatibleProvider.class);

    private final ObjectMapper objectMapper = new ObjectMapper();

    private final String baseUrl;
    private final String apiKey;
    private final String model;
    private final double temperature;
    private final int maxTokens;
    private final RestClient restClient;

    protected OpenAiCompatibleProvider(String baseUrl,
                                       String apiKey,
                                       String model,
                                       double temperature,
                                       int maxTokens,
                                       int connectTimeoutSeconds,
                                       int readTimeoutSeconds) {

        this.baseUrl = baseUrl;
        this.apiKey = apiKey == null ? "" : apiKey.trim();
        this.model = model;
        this.temperature = temperature;
        this.maxTokens = maxTokens;

        // Hard timeouts so the browser never waits forever.
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(Duration.ofSeconds(connectTimeoutSeconds));
        factory.setReadTimeout(Duration.ofSeconds(readTimeoutSeconds));

        this.restClient = RestClient.builder()
                .baseUrl(baseUrl)
                .requestFactory(factory)
                .build();
    }

    @Override
    public boolean isConfigured() {
        return !apiKey.isBlank();
    }

    protected String getModel() {
        return model;
    }

    @Override
    public String generate(String systemPrompt, String userPrompt) {

        if (!isConfigured()) {
            throw new AiException(name(),
                    name() + " has no API key configured.", true, 0);
        }

        Map<String, Object> body = Map.of(
                "model", model,
                "temperature", temperature,
                "max_tokens", maxTokens,
                "messages", List.of(
                        Map.of("role", "system", "content", systemPrompt),
                        Map.of("role", "user", "content", userPrompt)
                )
        );

        long startedAt = System.currentTimeMillis();
        log.info("AI request started | provider={} | model={} | promptChars={}",
                name(), model, systemPrompt.length() + userPrompt.length());

        String rawResponse;
        try {
            rawResponse = restClient.post()
                    .uri("/chat/completions")
                    .header("Authorization", "Bearer " + apiKey)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body)
                    .exchange((request, response) -> {
                        int status = response.getStatusCode().value();
                        String text = new String(response.getBody().readAllBytes());

                        if (status >= 200 && status < 300) {
                            return text;
                        }
                        throw toAiException(status, text);
                    });

        } catch (ResourceAccessException networkFailure) {
            // Connection refused, DNS failure, or our read timeout fired.
            log.error("AI request failed | provider={} | network/timeout | {}",
                    name(), networkFailure.getMessage());
            throw new AiException(name(),
                    name() + " could not be reached or timed out.", true, 0, networkFailure);
        }

        String content = extractContent(rawResponse);

        log.info("AI response received | provider={} | model={} | ms={} | responseChars={}",
                name(), model, System.currentTimeMillis() - startedAt, content.length());

        return content;
    }

    /** Turn a non-2xx HTTP reply into a typed, safe AiException. */
    private AiException toAiException(int status, String responseBody) {

        String detail = shorten(responseBody);

        // Rate limits, overload and gateway errors are worth retrying elsewhere.
        boolean retryable = status == 408 || status == 409 || status == 425
                         || status == 429 || status >= 500;

        // 403 from Groq almost always means "your account cannot use this model".
        if (status == 403 || status == 404) {
            log.error("AI request failed | provider={} | model={} | status={} | "
                    + "This usually means the model is not enabled for your account. | {}",
                    name(), model, status, detail);
        } else {
            log.error("AI request failed | provider={} | status={} | {}", name(), status, detail);
        }

        return new AiException(name(),
                name() + " returned HTTP " + status + ": " + detail, retryable, status);
    }

    /** Pull choices[0].message.content out of the JSON reply. */
    private String extractContent(String rawResponse) {
        try {
            JsonNode root = objectMapper.readTree(rawResponse);
            JsonNode choices = root.path("choices");

            if (!choices.isArray() || choices.isEmpty()) {
                throw new AiException(name(),
                        name() + " returned no choices.", true, 502);
            }

            String content = choices.get(0).path("message").path("content").asText("");

            if (content.isBlank()) {
                throw new AiException(name(),
                        name() + " returned an empty answer.", true, 502);
            }
            return content.trim();

        } catch (AiException alreadyTyped) {
            throw alreadyTyped;
        } catch (Exception parseFailure) {
            throw new AiException(name(),
                    name() + " returned a response we could not parse.", true, 502, parseFailure);
        }
    }

    /** Keep logs and error messages short, and never echo a key back. */
    private String shorten(String text) {
        if (text == null) return "";
        String cleaned = text.replaceAll("(gsk_|hf_|sk-)[A-Za-z0-9_\\-]+", "***REDACTED***");
        return cleaned.length() > 400 ? cleaned.substring(0, 400) + "..." : cleaned;
    }
}
