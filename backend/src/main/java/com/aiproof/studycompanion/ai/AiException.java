package com.aiproof.studycompanion.ai;

/**
 * Every AI failure becomes one of these.
 *
 * "retryable" answers one question: is it worth trying the other provider?
 *   - rate limit, timeout, provider outage  -> true
 *   - bad API key, unknown model, bad code  -> false
 */
public class AiException extends RuntimeException {

    private final boolean retryable;
    private final String providerName;
    private final int httpStatus;

    public AiException(String providerName, String message, boolean retryable, int httpStatus) {
        super(message);
        this.providerName = providerName;
        this.retryable = retryable;
        this.httpStatus = httpStatus;
    }

    public AiException(String providerName, String message, boolean retryable, int httpStatus, Throwable cause) {
        super(message, cause);
        this.providerName = providerName;
        this.retryable = retryable;
        this.httpStatus = httpStatus;
    }

    public boolean isRetryable() {
        return retryable;
    }

    public String getProviderName() {
        return providerName;
    }

    public int getHttpStatus() {
        return httpStatus;
    }

    /** Message that is safe to show a student. Never contains keys. */
    public String getUserMessage() {
        return switch (httpStatus) {
            case 401, 403 -> "The AI service rejected our credentials or the selected model. "
                           + "Please check the server configuration.";
            case 404      -> "The configured AI model was not found. Please check the model name in application.properties.";
            case 408, 504 -> "The AI service took too long to answer. Please try again.";
            case 429      -> "The AI service is rate limited right now. Please wait a moment and try again.";
            case 0        -> "The AI service could not be reached. Please check your internet connection.";
            default       -> "AI service is temporarily unavailable. Please try again.";
        };
    }
}
