package com.rag.engine.ragsearchengine.model;

import java.util.List;

public class RagResponse {

    private final String answer;
    private final List<String> retrievedChunks;
    private final double faithfulness;
    private final double answerRelevancy;
    private final boolean cacheHit;

    public RagResponse(String answer, List<String> retrievedChunks,
                       double faithfulness, double answerRelevancy) {
        this(answer, retrievedChunks, faithfulness, answerRelevancy, false);
    }

    public RagResponse(String answer, List<String> retrievedChunks,
                       double faithfulness, double answerRelevancy, boolean cacheHit) {
        this.answer = answer;
        this.retrievedChunks = retrievedChunks;
        this.faithfulness = faithfulness;
        this.answerRelevancy = answerRelevancy;
        this.cacheHit = cacheHit;
    }

    public String getAnswer() { return answer; }
    public List<String> getRetrievedChunks() { return retrievedChunks; }
    public double getFaithfulness() { return faithfulness; }
    public double getAnswerRelevancy() { return answerRelevancy; }
    public boolean isCacheHit() { return cacheHit; }
}
