package com.ocr.questions.controller;

import com.ocr.questions.entity.Question;
import com.ocr.questions.service.OcrService;
import com.ocr.questions.service.QuestionParserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

@RestController
@RequestMapping("/questions")
public class QuestionController {

    @Autowired
    private OcrService ocrService;

    @Autowired
    private QuestionParserService questionParserService;

    /**
     * Upload a PDF → extract text → parse questions/options/answers → save to DB
     */
    @PostMapping("/upload")
    public ResponseEntity<List<Question>> uploadAndParseQuestions(
            @RequestParam("file") MultipartFile file) {
        try {
            // Step 1: Extract raw text from PDF
            String extractedText = ocrService.extractRawTextFromPdf(file);

            // Step 2: Parse questions from extracted text and save
            List<Question> questions = questionParserService.parseAndSave(
                    extractedText, file.getOriginalFilename()
            );

            return ResponseEntity.ok(questions);
        } catch (IOException e) {
            throw new RuntimeException("Failed to process PDF: " + e.getMessage());
        }
    }

    /**
     * Get all parsed questions
     */
    @GetMapping("/all")
    public ResponseEntity<List<Question>> getAllQuestions() {
        return ResponseEntity.ok(questionParserService.getAllQuestions());
    }

    /**
     * Get questions from a specific file
     */
    @GetMapping("/file/{fileName}")
    public ResponseEntity<List<Question>> getByFile(@PathVariable String fileName) {
        return ResponseEntity.ok(questionParserService.getQuestionsByFile(fileName));
    }

    /**
     * Get a single question by ID
     */
    @GetMapping("/{id}")
    public ResponseEntity<Question> getById(@PathVariable Long id) {
        return ResponseEntity.ok(questionParserService.getQuestionById(id));
    }

    /**
     * Delete a question by ID
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<String> deleteQuestion(@PathVariable Long id) {
        questionParserService.deleteQuestion(id);
        return ResponseEntity.ok("Question deleted successfully");
    }
}

