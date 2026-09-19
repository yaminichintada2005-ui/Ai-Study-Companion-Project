package com.aiproof.studycompanion.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * Request body for every /api/ai endpoint.
 *
 * projectId and materialId are optional. When the AI Tutor sends them,
 * the answer is grounded in that study material instead of being a
 * generic chatbot reply.
 */
public record AiRequest(

        @NotBlank(message = "Message is required")
        String message,

        Long projectId,

        Long materialId

) {
}
