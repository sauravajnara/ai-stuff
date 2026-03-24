package com.rag.engine.ragsearchengine.controller;

import com.rag.engine.ragsearchengine.model.IngestedDocument;
import com.rag.engine.ragsearchengine.repository.IngestedDocumentRepository;
import com.rag.engine.ragsearchengine.service.DocumentIngestionService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.Set;

@RestController
@RequestMapping("/api/documents")
public class DocumentController {

    private static final Set<String> ALLOWED_EXTENSIONS = Set.of(".pdf", ".docx", ".doc", ".txt");

    private final DocumentIngestionService ingestionService;
    private final IngestedDocumentRepository documentRepository;

    public DocumentController(DocumentIngestionService ingestionService,
                               IngestedDocumentRepository documentRepository) {
        this.ingestionService = ingestionService;
        this.documentRepository = documentRepository;
    }

    @PostMapping("/upload")
    public ResponseEntity<IngestedDocument> upload(@RequestParam("file") MultipartFile file) throws IOException {
        if (file.isEmpty()) throw new IllegalArgumentException("File must not be empty");
        String filename = file.getOriginalFilename() != null ? file.getOriginalFilename().toLowerCase() : "";
        if (ALLOWED_EXTENSIONS.stream().noneMatch(filename::endsWith))
            throw new IllegalArgumentException("Unsupported file type. Allowed: pdf, docx, doc, txt");
        return ResponseEntity.ok(ingestionService.ingest(file));
    }

    @GetMapping
    public ResponseEntity<List<IngestedDocument>> list() {
        return ResponseEntity.ok(documentRepository.findAll());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        if (!documentRepository.existsById(id))
            throw new IllegalArgumentException("Document not found with id: " + id);
        documentRepository.deleteById(id);
        return ResponseEntity.noContent().build();
    }
}
