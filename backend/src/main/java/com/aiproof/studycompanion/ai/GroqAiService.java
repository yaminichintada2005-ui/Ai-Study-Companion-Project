package com.aiproof.studycompanion.ai;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * Primary provider: GroqCloud.
 *
 * NOTE ON MODELS (checked against console.groq.com/docs/models):
 * Llama models such as llama-3.3-70b-versatile and llama-3.1-8b-instant are
 * now Enterprise-only. On a normal developer account they return HTTP 403.
 * The generally available production chat models are:
 *     openai/gpt-oss-20b    (fastest, cheapest - our default)
 *     openai/gpt-oss-120b   (stronger, slower)
 */
@Service
public class GroqAiService extends OpenAiCompatibleProvider {

    public GroqAiService(
            @Value("${groq.api.url:https://api.groq.com/openai/v1}") String baseUrl,
            @Value("${groq.api.key:}") String apiKey,
            @Value("${groq.model:openai/gpt-oss-20b}") String model,
            @Value("${ai.temperature:0.3}") double temperature,
            @Value("${ai.max-tokens:2000}") int maxTokens,
            @Value("${ai.connect-timeout-seconds:10}") int connectTimeout,
            @Value("${ai.read-timeout-seconds:60}") int readTimeout) {

        super(baseUrl, apiKey, model, temperature, maxTokens, connectTimeout, readTimeout);
    }

    @Override
    public String name() {
        return "GROQ";
    }
}
