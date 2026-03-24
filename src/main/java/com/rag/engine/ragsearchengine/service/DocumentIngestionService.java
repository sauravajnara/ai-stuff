package com.rag.engine.ragsearchengine.service;

import com.rag.engine.ragsearchengine.model.IngestedDocument;
import com.rag.engine.ragsearchengine.repository.IngestedDocumentRepository;
import dev.langchain4j.data.document.Document;
import dev.langchain4j.data.document.DocumentParser;
import dev.langchain4j.data.document.parser.TextDocumentParser;
import dev.langchain4j.data.document.parser.apache.pdfbox.ApachePdfBoxDocumentParser;
import dev.langchain4j.data.document.parser.apache.poi.ApachePoiDocumentParser;
import dev.langchain4j.data.document.splitter.DocumentSplitters;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.embedding.EmbeddingStoreIngestor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

@Service
public class DocumentIngestionService {

    private final EmbeddingStore<TextSegment> embeddingStore;
    private final EmbeddingModel embeddingModel;
    private final IngestedDocumentRepository documentRepository;

    public DocumentIngestionService(EmbeddingStore<TextSegment> embeddingStore,
                                    EmbeddingModel embeddingModel,
                                    IngestedDocumentRepository documentRepository) {
        this.embeddingStore = embeddingStore;
        this.embeddingModel = embeddingModel;
        this.documentRepository = documentRepository;
    }

    public IngestedDocument ingest(MultipartFile file) throws IOException {
        String filename = file.getOriginalFilename() != null ? file.getOriginalFilename().toLowerCase() : "unknown";
        DocumentParser parser = resolveParser(filename);
        Document document = parser.parse(file.getInputStream());

        List<dev.langchain4j.data.segment.TextSegment> chunks = DocumentSplitters
                .recursive(500, 50).split(document);

        EmbeddingStoreIngestor.builder()
                .embeddingModel(embeddingModel)
                .embeddingStore(embeddingStore)
                .build()
                .ingest(document);

        IngestedDocument record = new IngestedDocument(filename, chunks.size(), "COMPLETED");
        return documentRepository.save(record);
    }

    private DocumentParser resolveParser(String filename) {
        if (filename.endsWith(".pdf")) return new ApachePdfBoxDocumentParser();
        if (filename.endsWith(".docx") || filename.endsWith(".doc")) return new ApachePoiDocumentParser();
        return new TextDocumentParser();
    }
}
