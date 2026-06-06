package com.ocr.questions.controller;

import com.ocr.questions.entity.Test;
import com.ocr.questions.repository.TestRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/test")
public class TestController {

    @Autowired
    private TestRepository testRepository;

    // Add a new record to the database
    @PostMapping("/add")
    public Test addTest(@RequestBody Test test) {

        return testRepository.save(test);
    }

    // Get all records from the database
    @GetMapping("/all")
    public List<Test> getAllTests() {
        return testRepository.findAll();
    }
}

