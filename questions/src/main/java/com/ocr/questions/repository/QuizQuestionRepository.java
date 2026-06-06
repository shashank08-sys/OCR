package com.ocr.questions.repository;

import com.ocr.questions.entity.QuizQuestion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface QuizQuestionRepository extends JpaRepository<QuizQuestion, Long> {

    List<QuizQuestion> findByTestIdAndCorrectIndexIsNotNull(String testId);

    List<QuizQuestion> findBySubjectAndCorrectIndexIsNotNull(String subject);

    List<QuizQuestion> findByTestIdAndSubjectAndCorrectIndexIsNotNull(String testId, String subject);

    void deleteByTestId(String testId);

    boolean existsByTestId(String testId);

    @Query("SELECT DISTINCT q.testId FROM QuizQuestion q WHERE q.correctIndex IS NOT NULL ORDER BY q.testId")
    List<String> findAllDistinctTestIds();

    @Query("SELECT DISTINCT q.subject FROM QuizQuestion q WHERE q.subject IS NOT NULL AND q.correctIndex IS NOT NULL ORDER BY q.subject")
    List<String> findAllDistinctSubjects();

    @Query("SELECT q FROM QuizQuestion q WHERE q.correctIndex IS NOT NULL")
    List<QuizQuestion> findAllWithCorrectIndex();
}

