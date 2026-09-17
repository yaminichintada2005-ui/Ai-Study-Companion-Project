package com.aiproof.studycompanion.service;

import com.aiproof.studycompanion.entity.Material;
import org.springframework.stereotype.Service;

@Service
public class MaterialAiService {

    private final MaterialService materialService;
    private final AiService aiService;

    public MaterialAiService(
            MaterialService materialService,
            AiService aiService) {

        this.materialService = materialService;
        this.aiService = aiService;
    }

    public String summarizeMaterial(Long materialId) {

        Material material =
                materialService.getMaterialById(materialId);

        validateContent(material);

        String prompt = """
                You are an AI study assistant.

                Summarize the following study material clearly.

                Requirements:
                - Identify the main concepts.
                - Use simple language.
                - Organize the answer with headings and bullet points.
                - Do not add information that is not present in the material.

                Study Material:
                %s
                """.formatted(material.getContent());

        return aiService.askAi(prompt);
    }

    public String explainMaterial(Long materialId) {

        Material material =
                materialService.getMaterialById(materialId);

        validateContent(material);

        String prompt = """
                You are an AI study assistant.

                Explain the following study material so that a student
                can understand it easily.

                Requirements:
                - Explain the important concepts step by step.
                - Use simple language.
                - Give examples only when they help explain the material.
                - Stay focused on the provided material.

                Study Material:
                %s
                """.formatted(material.getContent());

        return aiService.askAi(prompt);
    }

    public String generateQuiz(Long materialId) {

        Material material =
                materialService.getMaterialById(materialId);

        validateContent(material);

        String prompt = """
                You are an AI study assistant.

                Create a quiz based only on the following study material.

                Requirements:
                - Create 5 questions.
                - Use multiple-choice questions.
                - Provide 4 options for each question.
                - Clearly identify the correct answer.
                - Include a short explanation for each answer.
                - Do not ask questions about information outside the material.

                Study Material:
                %s
                """.formatted(material.getContent());

        return aiService.askAi(prompt);
    }

    private void validateContent(Material material) {

        if (material.getContent() == null
                || material.getContent().isBlank()) {

            throw new IllegalStateException(
                    "This material does not contain extracted text"
            );
        }
    }
}
