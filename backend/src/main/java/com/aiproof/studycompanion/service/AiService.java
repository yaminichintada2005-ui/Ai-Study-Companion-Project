package com.aiproof.studycompanion.service;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;

@Service
public class AiService {

    private final ChatClient chatClient;

    public AiService(ChatClient.Builder chatClientBuilder) {
        this.chatClient = chatClientBuilder.build();
    }

    // General AI question
    public String askAi(String message) {

        return chatClient
                .prompt()
                .user(message)
                .call()
                .content();
    }

    // Explain a topic for a student
    public String explainTopic(String topic) {

        String prompt = """
                You are an AI Study Companion.

                Explain the following topic clearly for a student:

                Topic:
                %s

                Requirements:
                - Use simple language.
                - Explain the important concepts.
                - Give a small example where useful.
                - Organize the answer with headings or bullet points.
                - Do not make the explanation unnecessarily complicated.
                """.formatted(topic);

        return chatClient
                .prompt()
                .user(prompt)
                .call()
                .content();
    }

    // Summarize study material
    public String summarize(String text) {

        String prompt = """
                You are an AI Study Companion.

                Summarize the following study material.

                Study Material:
                %s

                Requirements:
                - Keep the important concepts.
                - Remove unnecessary repetition.
                - Use clear headings and bullet points.
                - Make the summary useful for exam preparation.
                """.formatted(text);

        return chatClient
                .prompt()
                .user(prompt)
                .call()
                .content();
    }

    // Generate quiz questions
    public String generateQuiz(String topic) {

        String prompt = """
                You are an AI Study Companion.

                Create a short quiz about:

                Topic:
                %s

                Requirements:
                - Create 5 questions.
                - Use multiple-choice questions.
                - Give 4 options for each question.
                - Clearly indicate the correct answer.
                - Add a short explanation for each answer.
                """.formatted(topic);

        return chatClient
                .prompt()
                .user(prompt)
                .call()
                .content();
    }
}