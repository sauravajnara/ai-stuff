package com.rag.engine.ragsearchengine.model;

import java.util.List;

public class RagResponse {

    private final String answer;
    private final List<String> retrievedChunks;
    private final double faithfulness;
    private final double answerRelevancy;

    public RagResponse(String answer, List<String> retrievedChunks,
                       double faithfulness, double answerRelevancy) {
        this.answer = answer;
        this.retrievedChunks = retrievedChunks;
        this.faithfulness = faithfulness;
        this.answerRelevancy = answerRelevancy;
    }

    public String getAnswer() { return answer; }
    public List<String> getRetrievedChunks() { return retrievedChunks; }
    public double getFaithfulness() { return faithfulness; }
    public double getAnswerRelevancy() { return answerRelevancy; }
}
