package com.rag.engine.ragsearchengine.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "ingested_documents")
public class IngestedDocument {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String filename;
    private int chunkCount;
    private String status;
    private LocalDateTime uploadedAt;

    @PrePersist
    public void prePersist() { this.uploadedAt = LocalDateTime.now(); }

    public IngestedDocument() {}

    public IngestedDocument(String filename, int chunkCount, String status) {
        this.filename = filename;
        this.chunkCount = chunkCount;
        this.status = status;
    }

    public Long getId() { return id; }
    public String getFilename() { return filename; }
    public int getChunkCount() { return chunkCount; }
    public String getStatus() { return status; }
    public LocalDateTime getUploadedAt() { return uploadedAt; }
}
