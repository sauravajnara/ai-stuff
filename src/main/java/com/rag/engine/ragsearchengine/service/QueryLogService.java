package com.rag.engine.ragsearchengine.service;

import com.rag.engine.ragsearchengine.repository.QueryLogRepository;
import com.rag.engine.ragsearchengine.model.QueryLog;
import com.rag.engine.ragsearchengine.model.RagStats;

import org.springframework.stereotype.Service;

@Service
public class QueryLogService {

    private final QueryLogRepository repository;

    public QueryLogService(QueryLogRepository repository) {
        this.repository = repository;
    }

    public void save(String question, String answer, long latencyMs,
                     double avgChunkScore, double faithfulness, double answerRelevancy) {
        repository.save(new QueryLog(question, answer, latencyMs,
                avgChunkScore, faithfulness, answerRelevancy));
    }

    public RagStats getStats() {
        return new RagStats(
                nullSafe(repository.avgFaithfulness()),
                nullSafe(repository.avgAnswerRelevancy()),
                nullSafe(repository.avgLatencyMs()),
                nullSafe(repository.avgChunkScore()),
                repository.count()
        );
    }

    private double nullSafe(Double value) {
        return value != null ? value : 0.0;
    }
}
