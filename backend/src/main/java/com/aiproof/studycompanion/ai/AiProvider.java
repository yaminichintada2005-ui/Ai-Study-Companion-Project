package com.aiproof.studycompanion.ai;

/**
 * One AI provider (Groq, Hugging Face, ...).
 *
 * Everything above this interface (MaterialAiService, AiService) only ever
 * talks to THIS. It never knows which company is actually answering.
 */
public interface AiProvider {

    /** Short name used in logs, e.g. "GROQ". */
    String name();

    /** True when an API key is configured for this provider. */
    boolean isConfigured();

    /**
     * Send one prompt and get the text answer back.
     *
     * @param systemPrompt the role/rules given to the model
     * @param userPrompt   the actual task
     * @throws AiException when the provider fails
     */
    String generate(String systemPrompt, String userPrompt);
}
