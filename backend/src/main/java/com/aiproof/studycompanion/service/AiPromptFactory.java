package com.aiproof.studycompanion.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Builds every prompt in one place, and keeps the study material
 * small enough that the API stays fast and cheap.
 */
@Component
public class AiPromptFactory {

    private final int maxContentChars;

    public AiPromptFactory(@Value("${ai.max-content-chars:12000}") int maxContentChars) {
        this.maxContentChars = maxContentChars;
    }

    /**
     * A 40-page PDF can easily be 150,000 characters. Sending all of it is
     * slow and expensive, and blind truncation throws away the conclusion.
     *
     * So we keep the FIRST 70% and the LAST 30% of the budget. The beginning
     * of a document holds the definitions; the end holds the summary.
     */
    public String limitContent(String content) {
        if (content == null) return "";

        String cleaned = content.replaceAll("[ \\t]+", " ")
                                .replaceAll("\\n{3,}", "\n\n")
                                .trim();

        if (cleaned.length() <= maxContentChars) {
            return cleaned;
        }

        int headSize = (int) (maxContentChars * 0.7);
        int tailSize = maxContentChars - headSize;

        String head = cleaned.substring(0, headSize);
        String tail = cleaned.substring(cleaned.length() - tailSize);

        return head
             + "\n\n[... middle section omitted to stay within the AI size limit ...]\n\n"
             + tail;
    }

    public int getMaxContentChars() {
        return maxContentChars;
    }

    /* ================= SYSTEM PROMPTS ================= */

    public String summarySystemPrompt() {
        return """
               You are an AI Study Companion helping a student revise.
               You summarise ONLY the study material you are given.
               You never add facts from outside the material.
               You never invent information.
               If the material does not cover something, you say so.
               """;
    }

    public String explainSystemPrompt() {
        return """
               You are an AI Study Companion explaining material to a student
               who is seeing it for the first time.
               You explain ONLY what is in the study material given to you.
               You use simple language and short sentences.
               You never add facts from outside the material.
               """;
    }

    public String quizSystemPrompt() {
        return """
               You are an AI Study Companion that writes practice quizzes.
               You return ONLY raw JSON. No markdown, no code fences, no commentary.
               Every question must be answerable from the study material alone.
               """;
    }

    public String tutorSystemPrompt() {
        return """
               You are an AI Study Companion acting as a patient tutor.
               Answer using the student's study material as your main source.
               If the answer is not in the material, say so clearly,
               then give a short general explanation and label it as
               background knowledge rather than material content.
               Use simple language.
               """;
    }

    /* ================= USER PROMPTS ================= */

    public String summaryPrompt(String fileName, String content) {
        return """
               Summarise this study material for exam revision.

               Material name: %s

               Requirements:
               - Start with a one-paragraph overview.
               - Then use headings for each major topic.
               - Under each heading use short bullet points.
               - Keep every important concept, definition and term.
               - Remove repetition and filler.
               - Mark the most exam-important points with "IMPORTANT:".
               - Use simple language a student can read quickly.
               - Use only the material below. Do not invent anything.

               STUDY MATERIAL:
               %s
               """.formatted(fileName, limitContent(content));
    }

    public String explainPrompt(String fileName, String content) {
        return """
               Explain this study material to a student who is new to the topic.

               Material name: %s

               Requirements:
               - Use headings for each concept.
               - Break complex ideas into small numbered steps.
               - Give a short everyday example where it genuinely helps.
               - Define any technical term the first time it appears.
               - Use simple language and short sentences.
               - Use only the material below. Do not add outside information.

               STUDY MATERIAL:
               %s
               """.formatted(fileName, limitContent(content));
    }

    public String quizPrompt(String fileName, String content) {
        return """
               Create a practice quiz from the study material below.

               Return ONLY this JSON shape, with no text before or after it:

               {
                 "questions": [
                   {
                     "question": "the question text",
                     "options": ["option A", "option B", "option C", "option D"],
                     "correctIndex": 0,
                     "explanation": "one or two sentences saying why that option is correct",
                     "concept": "the topic this question tests"
                   }
                 ]
               }

               Rules:
               - EXACTLY 5 questions.
               - EXACTLY 4 options per question.
               - "correctIndex" is a number from 0 to 3 pointing at the correct option.
               - Vary which index is correct across the 5 questions.
               - Each question must test a DIFFERENT concept from the material.
               - Wrong options must be believable, not obviously silly.
               - Every question and answer must come from the material below.
               - Do not invent facts. Do not use markdown. Do not use code fences.

               Material name: %s

               STUDY MATERIAL:
               %s
               """.formatted(fileName, limitContent(content));
    }

    public String tutorPrompt(String fileName, String content, String question) {
        return """
               The student is studying this material and has asked a question.

               Material name: %s

               STUDY MATERIAL:
               %s

               STUDENT QUESTION:
               %s

               Answer clearly and simply. Where your answer comes from the
               material, say which part of it. Keep the answer focused on
               the question that was asked.
               """.formatted(fileName, limitContent(content), question);
    }
}
