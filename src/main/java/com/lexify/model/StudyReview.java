package com.lexify.model;

import jakarta.persistence.*;
import java.time.Instant;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "study_reviews")
@Getter
@Setter
@NoArgsConstructor
public class StudyReview {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "card_id", nullable = false)
    private StudyCard card;
    @Column(nullable = false)
    private int rating;
    @Column(nullable = false)
    private Instant reviewedAt;
    @Column(nullable = false)
    private Instant nextDue;
}
