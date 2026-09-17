package com.aiproof.studycompanion.dto;

import jakarta.validation.constraints.NotBlank;

public record AiRequest(

        @NotBlank(message = "Message is required")
        String message

) {
}