package com.ocr.questions.controller;

import com.ocr.questions.entity.QuizQuestion;
import com.ocr.questions.repository.QuizQuestionRepository;
import com.ocr.questions.service.QuizParserService;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/quiz")
public class QuizController {

    @Autowired
    private QuizQuestionRepository quizQuestionRepository;

    @Autowired
    private QuizParserService quizParserService;

    /**
     * POST /quiz/{testId}/upload?subject=Mathematics
     * Upload a PDF → parse questions → save & return as QuizQuestion list
     * Form-data: key=file, type=File
     * Query param: subject (optional) e.g. "Mathematics", "Reasoning"
     */
    @PostMapping("/{testId}/upload")
    public ResponseEntity<List<QuizQuestion>> uploadPdf(
            @PathVariable String testId,
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "subject", required = false) String subject) {
        try {
            List<QuizQuestion> questions = quizParserService.parsePdfAndSave(file, testId, subject);
            return ResponseEntity.ok(questions);
        } catch (IOException e) {
            throw new RuntimeException("Failed to process PDF: " + e.getMessage());
        }
    }

    /**
     * GET /quiz/subjects
     * Returns all distinct subjects
     */
    @GetMapping("/subjects")
    public ResponseEntity<List<String>> getAllSubjects() {
        return ResponseEntity.ok(quizQuestionRepository.findAllDistinctSubjects());
    }

    /**
     * GET /quiz/subject/{subject}
     * Returns all questions for a given subject across all tests
     */
    @GetMapping("/subject/{subject}")
    public ResponseEntity<List<QuizQuestion>> getQuestionsBySubject(@PathVariable String subject) {
        return ResponseEntity.ok(quizQuestionRepository.findBySubjectAndCorrectIndexIsNotNull(subject));
    }

    @GetMapping("/{testId}/subject/{subject}")
    public ResponseEntity<List<QuizQuestion>> getByTestIdAndSubject(
            @PathVariable String testId,
            @PathVariable String subject) {
        List<QuizQuestion> questions = quizQuestionRepository.findByTestIdAndSubjectAndCorrectIndexIsNotNull(testId, subject);
        if (questions.isEmpty()) return ResponseEntity.notFound().build();
        return ResponseEntity.ok(questions);
    }

    /**
     * GET /quiz/tests
     * Returns all distinct testIds
     * e.g. ["mock-1", "mock-2", "chapter-basics"]
     */
    @GetMapping("/tests")
    public ResponseEntity<List<String>> getAllTestIds() {
        return ResponseEntity.ok(quizQuestionRepository.findAllDistinctTestIds());
    }

    /**
     * GET /quiz/{testId}
     * Returns all questions for a given testId
     * e.g. GET /quiz/mock-1
     */
    @GetMapping("/{testId}")
    public ResponseEntity<List<QuizQuestion>> getQuestionsByTestId(@PathVariable String testId) {
        List<QuizQuestion> questions = quizQuestionRepository.findByTestIdAndCorrectIndexIsNotNull(testId);
        if (questions.isEmpty()) return ResponseEntity.notFound().build();
        return ResponseEntity.ok(questions);
    }

    @GetMapping("/all")
    public ResponseEntity<Map<String, List<QuizQuestion>>> getAllGrouped() {
        List<QuizQuestion> all = quizQuestionRepository.findAllWithCorrectIndex();
        Map<String, List<QuizQuestion>> grouped = all.stream()
                .collect(Collectors.groupingBy(QuizQuestion::getTestId));
        return ResponseEntity.ok(grouped);
    }

    /**
     * POST /quiz/{testId}/add
     * Add a single question to a test
     * Body: { "text": "...", "options": ["A","B","C","D"], "correctIndex": 1 }
     */
    @PostMapping("/{testId}/add")
    public ResponseEntity<QuizQuestion> addQuestion(
            @PathVariable String testId,
            @RequestBody QuizQuestion question) {
        question.setTestId(testId);
        return ResponseEntity.ok(quizQuestionRepository.save(question));
    }

    /**
     * POST /quiz/{testId}/bulk
     * Add multiple questions at once to a test
     * Body: [ { "text": "...", "options": [...], "correctIndex": 1 }, ... ]
     */
    @PostMapping("/{testId}/bulk")
    public ResponseEntity<List<QuizQuestion>> addBulkQuestions(
            @PathVariable String testId,
            @RequestBody List<QuizQuestion> questions) {
        questions.forEach(q -> q.setTestId(testId));
        return ResponseEntity.ok(quizQuestionRepository.saveAll(questions));
    }

    /**
     * PUT /quiz/{id}
     * Update a question by its DB id
     */
    @PutMapping("/{id}")
    public ResponseEntity<QuizQuestion> updateQuestion(
            @PathVariable Long id,
            @RequestBody QuizQuestion updated) {
        QuizQuestion existing = quizQuestionRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Question not found with id: " + id));
        existing.setText(updated.getText());
        existing.setOptions(updated.getOptions());
        existing.setCorrectIndex(updated.getCorrectIndex());
        existing.setSubject(updated.getSubject());
        return ResponseEntity.ok(quizQuestionRepository.save(existing));
    }

    /**
     * DELETE /quiz/{id}
     * Delete a single question by DB id
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<String> deleteQuestion(@PathVariable Long id) {
        quizQuestionRepository.deleteById(id);
        return ResponseEntity.ok("Question deleted successfully");
    }

    /**
     * DELETE /quiz/test/{testId}
     * Delete all questions for a testId
     */
    @Transactional
    @DeleteMapping("/test/{testId}")
    public ResponseEntity<String> deleteTest(@PathVariable String testId) {
        quizQuestionRepository.deleteByTestId(testId);
        return ResponseEntity.ok("All questions for '" + testId + "' deleted");
    }
}
