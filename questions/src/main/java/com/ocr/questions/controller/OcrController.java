package com.ocr.questions.controller;

import com.ocr.questions.entity.OcrResult;
import com.ocr.questions.service.OcrService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

@RestController
@RequestMapping("/ocr")
public class OcrController {

    @Autowired
    private OcrService ocrService;

    /**
     * Upload a PDF and extract text from it
     */
    @PostMapping("/pdf")
    public ResponseEntity<OcrResult> extractFromPdf(@RequestParam("file") MultipartFile file) {
        try {
            OcrResult result = ocrService.extractTextFromPdf(file);
            return ResponseEntity.ok(result);
        } catch (IOException e) {
            throw new RuntimeException("Failed to process PDF: " + e.getMessage());
        }
    }

    @GetMapping("/results")
    public ResponseEntity<List<OcrResult>> getAllResults() {
        return ResponseEntity.ok(ocrService.getAllResults());
    }

    @GetMapping("/results/{id}")
    public ResponseEntity<OcrResult> getResultById(@PathVariable Long id) {
        return ResponseEntity.ok(ocrService.getResultById(id));
    }

    @GetMapping("/results/type/{fileType}")
    public ResponseEntity<List<OcrResult>> getResultsByType(@PathVariable String fileType) {
        return ResponseEntity.ok(ocrService.getResultsByType(fileType));
    }

    @DeleteMapping("/results/{id}")
    public ResponseEntity<String> deleteResult(@PathVariable Long id) {
        ocrService.deleteResult(id);
        return ResponseEntity.ok("Deleted successfully");
    }
}
