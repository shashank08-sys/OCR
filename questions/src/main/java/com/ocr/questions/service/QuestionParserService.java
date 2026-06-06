package com.ocr.questions.service;

import com.ocr.questions.entity.Question;
import com.ocr.questions.repository.QuestionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class QuestionParserService {

    @Autowired
    private QuestionRepository questionRepository;

    /**
     * Parse extracted PDF text into Question objects and save to DB
     */
    public List<Question> parseAndSave(String extractedText, String sourceFile) {
        List<Question> questions = parseQuestions(extractedText, sourceFile);
        return questionRepository.saveAll(questions);
    }

    /**
     * Get all questions from DB
     */
    public List<Question> getAllQuestions() {
        return questionRepository.findAll();
    }

    /**
     * Get questions by source file name
     */
    public List<Question> getQuestionsByFile(String sourceFile) {
        return questionRepository.findBySourceFile(sourceFile);
    }

    /**
     * Get a single question by ID
     */
    public Question getQuestionById(Long id) {
        return questionRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Question not found with id: " + id));
    }

    /**
     * Delete a question by ID
     */
    public void deleteQuestion(Long id) {
        questionRepository.deleteById(id);
    }

    // ─── Core Parser ───────────────────────────────────────────────────────────

    private List<Question> parseQuestions(String text, String sourceFile) {
        List<Question> questions = new ArrayList<>();

        // Normalize line endings
        text = text.replace("\r\n", "\n").replace("\r", "\n");

        // Split at question start: Q1. | Q2. | Q10. etc.
        String[] blocks = text.split("(?m)(?=^Q\\d+\\.)");

        for (String block : blocks) {
            block = block.trim();
            if (block.isEmpty()) continue;

            Question q = parseBlock(block, sourceFile);
            if (q != null && q.getQuestionText() != null && !q.getQuestionText().isBlank()) {
                questions.add(q);
            }
        }

        return questions;
    }

    private Question parseBlock(String block, String sourceFile) {
        // Options pattern: (a) Option text  OR  (A) Option text
        Pattern optionPattern = Pattern.compile(
                "^\\s*\\(([a-dA-D])\\)\\s*(.+)$"
        );

        // Answer pattern: Ans.(a)  |  Ans. (a)  |  Answer: (a)  |  Ans: a  |  Ans.(A)
        Pattern answerPattern = Pattern.compile(
                "(?i)^\\s*Ans[wer]*\\.?\\s*\\(?([a-dA-D])\\)?.*$"
        );

        String optionA = null, optionB = null, optionC = null, optionD = null;
        String correctAnswer = null;
        StringBuilder questionBuilder = new StringBuilder();
        boolean optionSectionStarted = false;

        String[] lines = block.split("\n");
        boolean firstLine = true;

        for (String line : lines) {
            line = line.trim();
            if (line.isEmpty()) continue;

            // 1. Check for answer line: Ans.(a)
            Matcher answerMatcher = answerPattern.matcher(line);
            if (answerMatcher.matches()) {
                correctAnswer = answerMatcher.group(1).toUpperCase();
                continue;
            }

            // 2. Check for option line: (a) text
            Matcher optionMatcher = optionPattern.matcher(line);
            if (optionMatcher.matches()) {
                optionSectionStarted = true;
                String letter = optionMatcher.group(1).toUpperCase();
                String optText = optionMatcher.group(2).trim();

                switch (letter) {
                    case "A" -> optionA = optText;
                    case "B" -> optionB = optText;
                    case "C" -> optionC = optText;
                    case "D" -> optionD = optText;
                }
                continue;
            }

            // 3. If options haven't started yet, it's part of the question text
            if (!optionSectionStarted) {
                if (firstLine) {
                    // Strip Q1. Q2. Q10. prefix
                    line = line.replaceFirst("^Q\\d+\\.\\s*", "").trim();
                    firstLine = false;
                }
                if (!line.isEmpty()) {
                    questionBuilder.append(line).append(" ");
                }
            }
        }

        String questionText = questionBuilder.toString().trim();
        if (questionText.isEmpty()) return null;

        Question q = new Question();
        q.setQuestionText(questionText);
        q.setOptionA(optionA);
        q.setOptionB(optionB);
        q.setOptionC(optionC);
        q.setOptionD(optionD);
        q.setCorrectAnswer(correctAnswer);
        q.setSourceFile(sourceFile);
        return q;
    }
}

