package com.lexify.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "words", uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "english_text", "russian_text"}))
@Getter
@Setter
@NoArgsConstructor
public class Word {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false, length = 500)
    private String russianText;

    @Column(nullable = false, length = 500)
    private String englishText;

    @Column(length = 1000)
    private String example;

    @Column(nullable = false, length = 2)
    private String sourceLanguage;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "dictionary_word")
    private DictionaryEntry dictionary;
}
