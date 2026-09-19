package com.aiproof.studycompanion.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.aiproof.studycompanion.ai.AiProviderRouter;
import com.aiproof.studycompanion.entity.Material;

/**
 * Material-based AI features.
 *
 * Flow for every method:
 *   1. MaterialService loads the material AND checks it belongs to this user
 *   2. read the extracted PDF text
 *   3. validate that text exists
 *   4. build the prompt (content is size-limited inside AiPromptFactory)
 *   5. send it through the provider router (Groq, or Hugging Face)
 *   6. return the answer
 *
 * There is no HTTP code in this class. It does not know Groq exists.
 */
@Service
public class MaterialAiService {

    private static final Logger log = LoggerFactory.getLogger(MaterialAiService.class);

    private final MaterialService materialService;
    private final AiProviderRouter aiProviderRouter;
    private final AiPromptFactory promptFactory;
    private final QuizJsonFormatter quizJsonFormatter;

    public MaterialAiService(MaterialService materialService,
                             AiProviderRouter aiProviderRouter,
                             AiPromptFactory promptFactory,
                             QuizJsonFormatter quizJsonFormatter) {

        this.materialService = materialService;
        this.aiProviderRouter = aiProviderRouter;
        this.promptFactory = promptFactory;
        this.quizJsonFormatter = quizJsonFormatter;
    }

    /* ===================== SUMMARIZE ===================== */

    public String summarizeMaterial(Long materialId, String email) {

        Material material = loadOwnedMaterial(materialId, email);
        String content = requireContent(material);

        log.info("AI summarize | materialId={} | contentChars={}", materialId, content.length());

        return aiProviderRouter.generate(
                promptFactory.summarySystemPrompt(),
                promptFactory.summaryPrompt(fileNameOf(material), content)
        );
    }

    /* ===================== EXPLAIN ===================== */

    public String explainMaterial(Long materialId, String email) {

        Material material = loadOwnedMaterial(materialId, email);
        String content = requireContent(material);

        log.info("AI explain | materialId={} | contentChars={}", materialId, content.length());

        return aiProviderRouter.generate(
                promptFactory.explainSystemPrompt(),
                promptFactory.explainPrompt(fileNameOf(material), content)
        );
    }

    /* ===================== QUIZ ===================== */

    public String generateQuiz(Long materialId, String email) {

        Material material = loadOwnedMaterial(materialId, email);
        String content = requireContent(material);
        String fileName = fileNameOf(material);

        log.info("AI quiz | materialId={} | contentChars={}", materialId, content.length());

        String rawAnswer = aiProviderRouter.generate(
                promptFactory.quizSystemPrompt(),
                promptFactory.quizPrompt(fileName, content)
        );

        // Returns clean JSON in exactly the shape the React quiz page reads.
        return quizJsonFormatter.format(rawAnswer, fileName);
    }

    /* ===================== HELPERS ===================== */

    private Material loadOwnedMaterial(Long materialId, String email) {
        // MaterialService already enforces "this material belongs to this user".
        return materialService.getMaterialById(materialId, email);
    }

    private String requireContent(Material material) {
        String content = material.getContent();

        if (content == null || content.isBlank()) {
            throw new IllegalStateException(
                "No extracted content available for this material. "
              + "The PDF may be a scanned image, or text extraction may have failed. "
              + "Try uploading a text-based PDF.");
        }
        return content;
    }

    private String fileNameOf(Material material) {
        String fileName = material.getFileName();
        return (fileName == null || fileName.isBlank()) ? "study material" : fileName;
    }
}
