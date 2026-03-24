package com.rag.engine.ragsearchengine.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import com.rag.engine.ragsearchengine.model.QueryLog;

public interface QueryLogRepository extends JpaRepository<QueryLog, Long> {

    @Query("SELECT AVG(q.faithfulness) FROM QueryLog q")
    Double avgFaithfulness();

    @Query("SELECT AVG(q.answerRelevancy) FROM QueryLog q")
    Double avgAnswerRelevancy();

    @Query("SELECT AVG(q.latencyMs) FROM QueryLog q")
    Double avgLatencyMs();

    @Query("SELECT AVG(q.avgChunkScore) FROM QueryLog q")
    Double avgChunkScore();
}
