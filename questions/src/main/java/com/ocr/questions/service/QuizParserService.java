package com.ocr.questions.service;

import com.ocr.questions.entity.QuizQuestion;
import com.ocr.questions.repository.QuizQuestionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class QuizParserService {

    private static final Logger log = LoggerFactory.getLogger(QuizParserService.class);

    @Autowired
    private OcrService ocrService;

    @Autowired
    private QuizQuestionRepository quizQuestionRepository;

    /**
     * Upload PDF → extract text → parse into QuizQuestion list → save to DB
     */
    public List<QuizQuestion> parsePdfAndSave(MultipartFile file, String testId, String subject) throws IOException {
        String rawText = ocrService.extractRawTextFromPdf(file);
        log.info("=== RAW EXTRACTED TEXT ===\n{}", rawText);
        List<QuizQuestion> questions = parseQuestions(rawText, testId, subject);

        // Only save questions that have a valid correctIndex
        List<QuizQuestion> validQuestions = questions.stream()
                .filter(q -> q.getCorrectIndex() != null)
                .toList();

        log.info("=== PARSED {} QUESTIONS, {} have valid correctIndex ===", questions.size(), validQuestions.size());
        validQuestions.forEach(q -> log.info("Q: {} | Answer index: {}", q.getText(), q.getCorrectIndex()));
        return quizQuestionRepository.saveAll(validQuestions);
    }

    // ─── Parser ────────────────────────────────────────────────────────────────

    private List<QuizQuestion> parseQuestions(String text, String testId, String subject) {
        List<QuizQuestion> result = new ArrayList<>();

        text = text.replace("\r\n", "\n").replace("\r", "\n");

        String[] blocks = text.split("(?m)(?=^Q\\d+\\.)");

        for (String block : blocks) {
            block = block.trim();
            if (block.isEmpty()) continue;

            QuizQuestion q = parseBlock(block, testId, subject);
            if (q != null && q.getText() != null && !q.getText().isBlank()) {
                result.add(q);
            }
        }

        return result;
    }

    private QuizQuestion parseBlock(String block, String testId, String subject) {
        // Option line: (a) text | (A) text
        Pattern optionPattern = Pattern.compile("^\\s*\\(([a-dA-D])\\)\\s*(.+)$");

        // Answer line: Ans.(a) | Ans. (b) | Answer: (c) | Ans: d | Ans (a) | ans.(a)
        Pattern answerPattern = Pattern.compile(
                "(?i)Ans(?:wer)?[.:\\s]*\\(?([a-dA-D])\\)?"
        );

        List<String> options = new ArrayList<>(Arrays.asList(null, null, null, null));
        Integer correctIndex = null;
        StringBuilder questionBuilder = new StringBuilder();
        boolean optionSectionStarted = false;
        boolean firstLine = true;

        for (String line : block.split("\n")) {
            line = line.trim();
            if (line.isEmpty()) continue;

            // 1. Answer line: Ans.(a)
            Matcher answerMatcher = answerPattern.matcher(line);
            if (answerMatcher.find()) {
                String letter = answerMatcher.group(1).toLowerCase();
                correctIndex = letter.charAt(0) - 'a'; // a=0, b=1, c=2, d=3
                continue;
            }

            // 2. Option line: (a) Uncle
            Matcher optionMatcher = optionPattern.matcher(line);
            if (optionMatcher.find()) {
                optionSectionStarted = true;
                int idx = optionMatcher.group(1).toLowerCase().charAt(0) - 'a';
                String optText = optionMatcher.group(2).trim();
                if (idx >= 0 && idx < 4) {
                    options.set(idx, optText);
                }
                continue;
            }

            // 3. Question text (before options start)
            if (!optionSectionStarted) {
                if (firstLine) {
                    // Strip Q1. Q2. prefix
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

        // Remove nulls from options list (keep only non-null entries)
        List<String> cleanOptions = options.stream()
                .filter(o -> o != null)
                .toList();

        QuizQuestion q = new QuizQuestion();
        q.setTestId(testId);
        q.setSubject(subject);
        q.setText(questionText);
        q.setOptions(cleanOptions);
        q.setCorrectIndex(correctIndex);
        return q;
    }
}

