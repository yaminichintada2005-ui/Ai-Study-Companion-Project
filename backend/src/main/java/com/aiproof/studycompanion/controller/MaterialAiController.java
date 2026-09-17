package com.aiproof.studycompanion.controller;

import com.aiproof.studycompanion.service.MaterialAiService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/materials")
public class MaterialAiController {

    private final MaterialAiService materialAiService;

    public MaterialAiController(
            MaterialAiService materialAiService) {

        this.materialAiService = materialAiService;
    }

    // Generate AI summary of a material
    @PostMapping("/{materialId}/ai/summary")
    public ResponseEntity<String> summarizeMaterial(
            @PathVariable Long materialId) {

        String result =
                materialAiService.summarizeMaterial(materialId);

        return ResponseEntity.ok(result);
    }

    // Generate AI explanation of a material
    @PostMapping("/{materialId}/ai/explain")
    public ResponseEntity<String> explainMaterial(
            @PathVariable Long materialId) {

        String result =
                materialAiService.explainMaterial(materialId);

        return ResponseEntity.ok(result);
    }

    // Generate AI quiz from a material
    @PostMapping("/{materialId}/ai/quiz")
    public ResponseEntity<String> generateQuiz(
            @PathVariable Long materialId) {

        String result =
                materialAiService.generateQuiz(materialId);

        return ResponseEntity.ok(result);
    }
}
