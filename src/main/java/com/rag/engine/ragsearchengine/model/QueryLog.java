package com.rag.engine.ragsearchengine.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "query_logs")
public class QueryLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(columnDefinition = "TEXT")
    private String question;

    @Column(columnDefinition = "TEXT")
    private String answer;

    private long latencyMs;
    private double avgChunkScore;
    private double faithfulness;
    private double answerRelevancy;
    private LocalDateTime createdAt;

    @PrePersist
    public void prePersist() {
        this.createdAt = LocalDateTime.now();
    }

    public QueryLog() {}

    public QueryLog(String question, String answer, long latencyMs,
                    double avgChunkScore, double faithfulness, double answerRelevancy) {
        this.question = question;
        this.answer = answer;
        this.latencyMs = latencyMs;
        this.avgChunkScore = avgChunkScore;
        this.faithfulness = faithfulness;
        this.answerRelevancy = answerRelevancy;
    }

    public Long getId() { return id; }
    public String getQuestion() { return question; }
    public String getAnswer() { return answer; }
    public long getLatencyMs() { return latencyMs; }
    public double getAvgChunkScore() { return avgChunkScore; }
    public double getFaithfulness() { return faithfulness; }
    public double getAnswerRelevancy() { return answerRelevancy; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}
