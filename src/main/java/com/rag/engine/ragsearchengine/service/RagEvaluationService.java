package com.rag.engine.ragsearchengine.service;

import dev.langchain4j.model.chat.ChatLanguageModel;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class RagEvaluationService {

    private final ChatLanguageModel chatModel;

    public RagEvaluationService(ChatLanguageModel chatModel) {
        this.chatModel = chatModel;
    }

    // Faithfulness: is the answer grounded in the context?
    public double faithfulness(String answer, List<String> chunks) {
        String context = String.join("\n\n", chunks);
        String prompt = """
                Given the context and answer below, rate how faithful the answer is to the context.
                Score from 0.0 (not faithful) to 1.0 (completely faithful). Reply with only a number.

                Context: %s

                Answer: %s
                """.formatted(context, answer);
        return parseScore(chatModel.chat(prompt));
    }

    // Answer Relevancy: does the answer address the question?
    public double answerRelevancy(String question, String answer) {
        String prompt = """
                Given the question and answer below, rate how relevant the answer is to the question.
                Score from 0.0 (not relevant) to 1.0 (highly relevant). Reply with only a number.

                Question: %s

                Answer: %s
                """.formatted(question, answer);
        return parseScore(chatModel.chat(prompt));
    }

    private double parseScore(String response) {
        try {
            return Double.parseDouble(response.trim().replaceAll("[^0-9.]", ""));
        } catch (NumberFormatException e) {
            return 0.0;
        }
    }
}
