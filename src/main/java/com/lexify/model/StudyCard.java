package com.lexify.model;

import jakarta.persistence.*;
import java.time.Instant;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "study_cards", uniqueConstraints = @UniqueConstraint(columnNames = {"word_id", "direction"}))
@Getter
@Setter
@NoArgsConstructor
public class StudyCard {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "word_id", nullable = false)
    private Word word;
    @Column(nullable = false, length = 2)
    private String direction;
    @Column(nullable = false, columnDefinition = "text")
    private String cardState;
    @Column(nullable = false)
    private Instant due;
    @Column(nullable = false)
    private int reviews;
}
