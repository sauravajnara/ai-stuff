package com.rag.engine.ragsearchengine.repository;

import com.rag.engine.ragsearchengine.model.IngestedDocument;
import org.springframework.data.jpa.repository.JpaRepository;

public interface IngestedDocumentRepository extends JpaRepository<IngestedDocument, Long> {
}
