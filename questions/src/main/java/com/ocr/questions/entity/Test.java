package com.ocr.questions.entity;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Data
@Table(name = "test")
public class Test {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;

    private String description;
}
