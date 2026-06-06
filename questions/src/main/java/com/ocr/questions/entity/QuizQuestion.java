package com.ocr.questions.entity;

import com.ocr.questions.converter.StringListConverter;
import jakarta.persistence.*;
import lombok.Data;

import java.util.List;

@Entity
@Data
@Table(name = "quiz_questions")
public class QuizQuestion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String testId;   // e.g. "mock-1", "chapter-basics"

    @Column
    private String subject;  // e.g. "Mathematics", "Reasoning", "English"

    @Column(columnDefinition = "TEXT", nullable = false)
    private String text;     // question text

    @Convert(converter = StringListConverter.class)
    @Column(columnDefinition = "TEXT", nullable = false)
    private List<String> options;  // ["option A", "option B", "option C", "option D"]

    @Column
    private Integer correctIndex;  // 0-based index of the correct option (nullable if not found in PDF)
}

