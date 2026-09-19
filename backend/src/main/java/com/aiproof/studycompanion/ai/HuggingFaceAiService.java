package com.aiproof.studycompanion.ai;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * Fallback provider: Hugging Face Inference Providers.
 *
 * HF exposes an OpenAI-compatible router at https://router.huggingface.co/v1,
 * so the request format is identical to Groq's.
 *
 * IMPORTANT: your token must be a FINE-GRAINED token created at
 * huggingface.co -> Settings -> Access Tokens, with the permission
 * "Make calls to Inference Providers" ticked. A plain read token gives 401/403.
 */
@Service
public class HuggingFaceAiService extends OpenAiCompatibleProvider {

    public HuggingFaceAiService(
            @Value("${huggingface.api.url:https://router.huggingface.co/v1}") String baseUrl,
            @Value("${huggingface.api.key:}") String apiKey,
            @Value("${huggingface.model:meta-llama/Llama-3.1-8B-Instruct}") String model,
            @Value("${ai.temperature:0.3}") double temperature,
            @Value("${ai.max-tokens:2000}") int maxTokens,
            @Value("${ai.connect-timeout-seconds:10}") int connectTimeout,
            @Value("${ai.read-timeout-seconds:60}") int readTimeout) {

        super(baseUrl, apiKey, model, temperature, maxTokens, connectTimeout, readTimeout);
    }

    @Override
    public String name() {
        return "HUGGINGFACE";
    }
}
