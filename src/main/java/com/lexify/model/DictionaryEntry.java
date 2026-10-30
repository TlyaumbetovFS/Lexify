package com.lexify.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "dictionary_entries")
@Getter
@Setter
@NoArgsConstructor
public class DictionaryEntry {

    @Id
    @Column(length = 500)
    private String word;

    @Column(length = 1000)
    private String example;

    @Column(length = 200)
    private String phonetic;

    @Column(length = 2000)
    private String audioUrl;

    @Column(length = 2000)
    private String sourceUrl;

    @Column(length = 2000)
    private String licenseUrl;

    @Column(length = 2000)
    private String audioSourceUrl;

    @Column(length = 2000)
    private String audioLicenseUrl;

    @Column(nullable = false)
    private Instant checkedAt;

    public boolean hasContent() {
        return example != null || phonetic != null || audioUrl != null;
    }
}
