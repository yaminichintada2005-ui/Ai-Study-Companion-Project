package com.aiproof.studycompanion.service;

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * Language models are not reliable JSON generators. They wrap answers in
 * ```json fences, add a friendly sentence first, or return "correctAnswer"
 * as text instead of "correctIndex" as a number.
 *
 * This class takes whatever the model produced and turns it into the EXACT
 * shape your React QuizResult component already reads:
 *
 * {
 *   "source": "SE_project.pdf",
 *   "questions": [
 *     { "number":1, "type":"MCQ", "concept":"...", "question":"...",
 *       "options":["a","b","c","d"], "correctIndex":2,
 *       "explanation":"...", "citation":"..." }
 *   ]
 * }
 */
@Component
public class QuizJsonFormatter {

    private static final Logger log = LoggerFactory.getLogger(QuizJsonFormatter.class);

    private final ObjectMapper objectMapper = new ObjectMapper();

    public String format(String rawModelOutput, String sourceFileName) {

        JsonNode parsed = parseLoosely(rawModelOutput);

        if (parsed == null || !parsed.path("questions").isArray()) {
            throw new IllegalStateException(
                "The AI did not return a usable quiz. Please press Generate again.");
        }

        ArrayNode cleanQuestions = objectMapper.createArrayNode();
        int number = 1;

        for (JsonNode question : parsed.path("questions")) {
            ObjectNode clean = cleanOneQuestion(question, number, sourceFileName);
            if (clean != null) {
                cleanQuestions.add(clean);
                number++;
            }
            if (cleanQuestions.size() == 5) break;
        }

        if (cleanQuestions.isEmpty()) {
            throw new IllegalStateException(
                "The AI returned a quiz with no valid questions. Please press Generate again.");
        }

        if (cleanQuestions.size() < 5) {
            log.warn("Quiz came back with only {} valid questions instead of 5.", cleanQuestions.size());
        }

        ObjectNode root = objectMapper.createObjectNode();
        root.put("source", sourceFileName);
        root.set("questions", cleanQuestions);

        return root.toPrettyString();
    }

    /* ---------- strip fences / prose and parse ---------- */

    private JsonNode parseLoosely(String raw) {
        if (raw == null || raw.isBlank()) return null;

        String text = raw.trim()
                         .replaceAll("(?s)^```(?:json)?", "")
                         .replaceAll("(?s)```$", "")
                         .trim();

        int start = text.indexOf('{');
        int end = text.lastIndexOf('}');
        if (start < 0 || end <= start) return null;

        try {
            return objectMapper.readTree(text.substring(start, end + 1));
        } catch (Exception failure) {
            log.warn("Quiz JSON could not be parsed: {}", failure.getMessage());
            return null;
        }
    }

    /* ---------- normalise one question ---------- */

    private ObjectNode cleanOneQuestion(JsonNode source, int number, String fileName) {

        String questionText = firstNonBlank(
                source.path("question").asText(""),
                source.path("text").asText(""),
                source.path("prompt").asText(""));

        if (questionText.isBlank()) return null;

        List<String> options = new ArrayList<>();
        for (JsonNode option : source.path("options")) {
            String value = option.isTextual() ? option.asText() : option.toString();
            if (!value.isBlank()) options.add(stripLeadingLabel(value.trim()));
        }
        if (options.size() < 2) return null;

        // Keep exactly 4 options when the model gave more.
        while (options.size() > 4) options.remove(options.size() - 1);

        int correctIndex = resolveCorrectIndex(source, options);
        if (correctIndex < 0 || correctIndex >= options.size()) {
            correctIndex = 0;
            log.warn("Question {} had no usable correct answer; defaulting to the first option.", number);
        }

        ObjectNode clean = objectMapper.createObjectNode();
        clean.put("number", number);
        clean.put("type", "MCQ");
        clean.put("question", questionText.trim());

        ArrayNode optionArray = objectMapper.createArrayNode();
        options.forEach(optionArray::add);
        clean.set("options", optionArray);

        clean.put("correctIndex", correctIndex);

        String explanation = firstNonBlank(
                source.path("explanation").asText(""),
                source.path("reason").asText(""),
                source.path("rationale").asText(""));
        if (!explanation.isBlank()) clean.put("explanation", explanation.trim());

        String concept = firstNonBlank(
                source.path("concept").asText(""),
                source.path("topic").asText(""));
        if (!concept.isBlank()) clean.put("concept", concept.trim());

        String difficulty = source.path("difficulty").asText("");
        if (!difficulty.isBlank()) clean.put("difficulty", difficulty.trim());

        clean.put("citation", "From " + fileName);

        return clean;
    }

    /**
     * The model might give us the answer as an index (0-3), as a letter ("C"),
     * or as the full option text. Handle all three.
     */
    private int resolveCorrectIndex(JsonNode source, List<String> options) {

        JsonNode indexNode = source.path("correctIndex");
        if (indexNode.isInt()) return indexNode.asInt();

        JsonNode answerNode = source.has("correctAnswer")
                ? source.path("correctAnswer")
                : source.path("answer");

        if (answerNode.isInt()) return answerNode.asInt();

        String answer = answerNode.asText("").trim();
        if (answer.isBlank()) return -1;

        // "C" or "c)" or "3"
        if (answer.length() <= 2) {
            char first = Character.toUpperCase(answer.charAt(0));
            if (first >= 'A' && first <= 'D') return first - 'A';
            if (Character.isDigit(first)) {
                int value = Character.getNumericValue(first);
                return value >= 1 && value <= options.size() ? value - 1 : value;
            }
        }

        // Full option text
        String normalisedAnswer = stripLeadingLabel(answer).toLowerCase();
        for (int i = 0; i < options.size(); i++) {
            if (options.get(i).toLowerCase().equals(normalisedAnswer)) return i;
        }
        for (int i = 0; i < options.size(); i++) {
            if (options.get(i).toLowerCase().contains(normalisedAnswer)) return i;
        }
        return -1;
    }

    /** Turn "B) Inheritance" into "Inheritance" so options render cleanly. */
    private String stripLeadingLabel(String option) {
        return option.replaceFirst("^\\s*[A-Da-d]\\s*[\\)\\.\\:-]\\s+", "")
                     .replaceFirst("^\\s*[1-4]\\s*[\\)\\.\\:-]\\s+", "")
                     .trim();
    }

    private String firstNonBlank(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) return value;
        }
        return "";
    }
}
