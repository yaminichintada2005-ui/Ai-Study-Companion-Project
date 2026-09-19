package com.aiproof.studycompanion.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.aiproof.studycompanion.dto.AiRequest;
import com.aiproof.studycompanion.dto.AiResponse;
import com.aiproof.studycompanion.service.AiService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/ai")
public class AiController {

    private final AiService aiService;

    public AiController(AiService aiService) {
        this.aiService = aiService;
    }

    /*
     * AI Tutor.
     * When the request carries a materialId, the answer is grounded in
     * that student's own study material. Ownership is checked inside
     * AiService via MaterialService.
     */
    @PostMapping("/ask")
    public ResponseEntity<AiResponse> askAi(
            @Valid @RequestBody AiRequest request,
            Authentication authentication) {

        String email = authentication.getName();

        String answer = aiService.askAi(
                request.message(),
                request.projectId(),
                request.materialId(),
                email
        );

        return ResponseEntity.ok(new AiResponse(answer));
    }

    // Explain a free-text topic.
    @PostMapping("/explain")
    public ResponseEntity<AiResponse> explainTopic(
            @Valid @RequestBody AiRequest request) {

        return ResponseEntity.ok(
                new AiResponse(aiService.explainTopic(request.message()))
        );
    }

    // Summarize free text.
    @PostMapping("/summarize")
    public ResponseEntity<AiResponse> summarize(
            @Valid @RequestBody AiRequest request) {

        return ResponseEntity.ok(
                new AiResponse(aiService.summarize(request.message()))
        );
    }

    // Build a quiz from free text.
    @PostMapping("/quiz")
    public ResponseEntity<AiResponse> generateQuiz(
            @Valid @RequestBody AiRequest request) {

        return ResponseEntity.ok(
                new AiResponse(aiService.generateQuiz(request.message()))
        );
    }
}
