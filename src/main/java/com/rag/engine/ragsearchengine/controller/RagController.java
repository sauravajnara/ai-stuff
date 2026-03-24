package com.rag.engine.ragsearchengine.controller;

import com.rag.engine.ragsearchengine.model.RagResponse;
import com.rag.engine.ragsearchengine.model.RagStats;
import com.rag.engine.ragsearchengine.service.QueryLogService;
import com.rag.engine.ragsearchengine.service.RagQueryService;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/rag")
public class RagController {

    private final RagQueryService ragQueryService;
    private final QueryLogService queryLogService;

    public RagController(RagQueryService ragQueryService, QueryLogService queryLogService) {
        this.ragQueryService = ragQueryService;
        this.queryLogService = queryLogService;
    }

    @GetMapping("/query")
    public ResponseEntity<String> query(@RequestParam("question") String question) {
        if (question == null || question.isBlank()) throw new IllegalArgumentException("Question must not be empty");
        if (question.length() > 500) throw new IllegalArgumentException("Question must not exceed 500 characters");
        return ResponseEntity.ok(ragQueryService.query(question));
    }

    @GetMapping("/query/evaluate")
    public ResponseEntity<RagResponse> queryWithEval(@RequestParam("question") String question) {
        if (question == null || question.isBlank()) throw new IllegalArgumentException("Question must not be empty");
        if (question.length() > 500) throw new IllegalArgumentException("Question must not exceed 500 characters");
        return ResponseEntity.ok(ragQueryService.queryWithEval(question));
    }

    @GetMapping("/stats")
    public ResponseEntity<RagStats> stats() {
        return ResponseEntity.ok(queryLogService.getStats());
    }
}
