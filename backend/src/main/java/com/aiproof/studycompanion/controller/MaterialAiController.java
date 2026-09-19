package com.aiproof.studycompanion.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.aiproof.studycompanion.service.MaterialAiService;

@RestController
@RequestMapping("/api/materials")
public class MaterialAiController {

    private final MaterialAiService materialAiService;

    public MaterialAiController(
            MaterialAiService materialAiService) {

        this.materialAiService = materialAiService;
    }

    // =========================================================
    // SUMMARIZE MATERIAL
    // POST /api/materials/{materialId}/summarize
    // =========================================================

    @PostMapping("/{materialId}/summarize")
    public ResponseEntity<String> summarizeMaterial(
            @PathVariable Long materialId,
            Authentication authentication) {

        String email = authentication.getName();

        String summary =
                materialAiService.summarizeMaterial(
                        materialId,
                        email
                );

        return ResponseEntity.ok(summary);
    }

    // =========================================================
    // EXPLAIN MATERIAL
    // POST /api/materials/{materialId}/explain
    // =========================================================

    @PostMapping("/{materialId}/explain")
    public ResponseEntity<String> explainMaterial(
            @PathVariable Long materialId,
            Authentication authentication) {

        String email = authentication.getName();

        String explanation =
                materialAiService.explainMaterial(
                        materialId,
                        email
                );

        return ResponseEntity.ok(explanation);
    }

    // =========================================================
    // GENERATE QUIZ
    // POST /api/materials/{materialId}/quiz
    // =========================================================

    @PostMapping("/{materialId}/quiz")
    public ResponseEntity<String> generateQuiz(
            @PathVariable Long materialId,
            Authentication authentication) {

        String email = authentication.getName();

        String quiz =
                materialAiService.generateQuiz(
                        materialId,
                        email
                );

        return ResponseEntity.ok(quiz);
    }
}