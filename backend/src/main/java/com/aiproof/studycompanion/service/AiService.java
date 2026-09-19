package com.aiproof.studycompanion.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.aiproof.studycompanion.ai.AiProviderRouter;
import com.aiproof.studycompanion.entity.Material;

/**
 * Free-text AI features, plus the grounded AI Tutor.
 *
 * Used by AiController:
 *   POST /api/ai/ask        -> askAi(...)      grounded on the student's material
 *   POST /api/ai/explain    -> explainTopic(...)
 *   POST /api/ai/summarize  -> summarize(...)
 *   POST /api/ai/quiz       -> generateQuiz(...)
 */
@Service
public class AiService {

    private static final Logger log = LoggerFactory.getLogger(AiService.class);

    private final MaterialService materialService;
    private final AiProviderRouter aiProviderRouter;
    private final AiPromptFactory promptFactory;

    public AiService(MaterialService materialService,
                     AiProviderRouter aiProviderRouter,
                     AiPromptFactory promptFactory) {

        this.materialService = materialService;
        this.aiProviderRouter = aiProviderRouter;
        this.promptFactory = promptFactory;
    }

    /* ===================== AI TUTOR (grounded) ===================== */

    public String askAi(String message, Long projectId, Long materialId, String email) {

        if (message == null || message.isBlank()) {
            throw new IllegalArgumentException("Please type a question.");
        }

        // No material selected: answer as a general tutor.
        if (materialId == null) {
            log.info("AI tutor | no material selected | projectId={}", projectId);
            return aiProviderRouter.generate(
                    promptFactory.tutorSystemPrompt(),
                    "The student has not selected any study material yet.\n\n"
                  + "STUDENT QUESTION:\n" + message
            );
        }

        // MaterialService enforces ownership for this user.
        Material material = materialService.getMaterialById(materialId, email);

        String content = material.getContent();
        String fileName = material.getFileName() == null ? "study material" : material.getFileName();

        if (content == null || content.isBlank()) {
            throw new IllegalStateException(
                "No extracted content available for this material, so the tutor "
              + "has nothing to read. Try uploading a text-based PDF.");
        }

        log.info("AI tutor | materialId={} | contentChars={}", materialId, content.length());

        return aiProviderRouter.generate(
                promptFactory.tutorSystemPrompt(),
                promptFactory.tutorPrompt(fileName, content, message)
        );
    }

    /* ===================== FREE TEXT HELPERS ===================== */

    public String explainTopic(String topic) {
        if (topic == null || topic.isBlank()) {
            throw new IllegalArgumentException("Please provide a topic to explain.");
        }
        return aiProviderRouter.generate(
                promptFactory.explainSystemPrompt(),
                "Explain this topic simply, for a student seeing it for the first time. "
              + "Use headings and short bullet points.\n\nTOPIC:\n" + topic
        );
    }

    public String summarize(String text) {
        if (text == null || text.isBlank()) {
            throw new IllegalArgumentException("Please provide some text to summarize.");
        }
        return aiProviderRouter.generate(
                promptFactory.summarySystemPrompt(),
                "Summarize the text below for exam revision. Use headings and "
              + "short bullet points, and keep every important concept.\n\nTEXT:\n"
              + promptFactory.limitContent(text)
        );
    }

    public String generateQuiz(String text) {
        if (text == null || text.isBlank()) {
            throw new IllegalArgumentException("Please provide some text to build a quiz from.");
        }
        return aiProviderRouter.generate(
                promptFactory.quizSystemPrompt(),
                promptFactory.quizPrompt("pasted text", text)
        );
    }
}
