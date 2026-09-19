package com.aiproof.studycompanion.ai;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;

/**
 * Decides WHICH provider answers.
 *
 * ai.provider=groq          -> Groq only
 * ai.provider=huggingface   -> Hugging Face only
 * ai.provider=auto          -> try Groq, fall back to Hugging Face
 *
 * Fallback only happens for provider-side problems (rate limit, outage,
 * timeout, missing key). A programming mistake is never hidden.
 */
@Service
public class AiProviderRouter {

    private static final Logger log = LoggerFactory.getLogger(AiProviderRouter.class);

    private final GroqAiService groq;
    private final HuggingFaceAiService huggingFace;
    private final String configuredProvider;

    public AiProviderRouter(GroqAiService groq,
                            HuggingFaceAiService huggingFace,
                            @Value("${ai.provider:groq}") String configuredProvider) {

        this.groq = groq;
        this.huggingFace = huggingFace;
        this.configuredProvider = configuredProvider == null
                ? "groq"
                : configuredProvider.trim().toLowerCase();
    }

    @PostConstruct
    public void reportConfiguration() {
        log.info("AI provider selected: {}", configuredProvider.toUpperCase());
        log.info("Groq API key present: {}", groq.isConfigured());
        log.info("Hugging Face API key present: {}", huggingFace.isConfigured());

        if (!groq.isConfigured() && !huggingFace.isConfigured()) {
            log.warn("NO AI API KEY IS CONFIGURED. "
                   + "Set GROQ_API_KEY (and optionally HF_API_KEY) before starting the backend.");
        }
    }

    public String generate(String systemPrompt, String userPrompt) {

        switch (configuredProvider) {

            case "huggingface":
                return huggingFace.generate(systemPrompt, userPrompt);

            case "auto":
                try {
                    return groq.generate(systemPrompt, userPrompt);

                } catch (AiException groqFailure) {

                    if (!groqFailure.isRetryable()) {
                        // Bad model name, bad key, bad request: do NOT hide it.
                        throw groqFailure;
                    }
                    if (!huggingFace.isConfigured()) {
                        throw groqFailure;
                    }

                    log.warn("Falling back to Hugging Face. Groq said: {}", groqFailure.getMessage());
                    return huggingFace.generate(systemPrompt, userPrompt);
                }

            case "groq":
            default:
                return groq.generate(systemPrompt, userPrompt);
        }
    }
}
