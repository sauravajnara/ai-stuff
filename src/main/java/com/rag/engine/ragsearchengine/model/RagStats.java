package com.rag.engine.ragsearchengine.model;

public class RagStats {

    private final double avgFaithfulness;
    private final double avgAnswerRelevancy;
    private final double avgLatencyMs;
    private final double avgChunkScore;
    private final long totalQueries;

    public RagStats(double avgFaithfulness, double avgAnswerRelevancy,
                    double avgLatencyMs, double avgChunkScore, long totalQueries) {
        this.avgFaithfulness = avgFaithfulness;
        this.avgAnswerRelevancy = avgAnswerRelevancy;
        this.avgLatencyMs = avgLatencyMs;
        this.avgChunkScore = avgChunkScore;
        this.totalQueries = totalQueries;
    }

    public double getAvgFaithfulness() { return avgFaithfulness; }
    public double getAvgAnswerRelevancy() { return avgAnswerRelevancy; }
    public double getAvgLatencyMs() { return avgLatencyMs; }
    public double getAvgChunkScore() { return avgChunkScore; }
    public long getTotalQueries() { return totalQueries; }
}
