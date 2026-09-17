package com.aiproof.studycompanion.controller;

import com.aiproof.studycompanion.dto.AiRequest;
import com.aiproof.studycompanion.dto.AiResponse;
import com.aiproof.studycompanion.service.AiService;

import jakarta.validation.Valid;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/ai")
public class AiController {

    private final AiService aiService;

    public AiController(AiService aiService) {
        this.aiService = aiService;
    }

    // General AI question
    @PostMapping("/ask")
    public ResponseEntity<AiResponse> askAi(
            @Valid @RequestBody AiRequest request) {

        String answer = aiService.askAi(request.message());

        return ResponseEntity.ok(
                new AiResponse(answer)
        );
    }

    // Explain a topic
    @PostMapping("/explain")
    public ResponseEntity<AiResponse> explainTopic(
            @Valid @RequestBody AiRequest request) {

        String answer = aiService.explainTopic(
                request.message()
        );

        return ResponseEntity.ok(
                new AiResponse(answer)
        );
    }

    // Summarize study material
    @PostMapping("/summarize")
    public ResponseEntity<AiResponse> summarize(
            @Valid @RequestBody AiRequest request) {

        String answer = aiService.summarize(
                request.message()
        );

        return ResponseEntity.ok(
                new AiResponse(answer)
        );
    }

    // Generate quiz
    @PostMapping("/quiz")
    public ResponseEntity<AiResponse> generateQuiz(
            @Valid @RequestBody AiRequest request) {

        String answer = aiService.generateQuiz(
                request.message()
        );

        return ResponseEntity.ok(
                new AiResponse(answer)
        );
    }
}