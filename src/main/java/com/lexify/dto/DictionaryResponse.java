package com.lexify.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record DictionaryResponse(String phonetic, List<Phonetic> phonetics, List<Meaning> meanings,
                                 License license, List<String> sourceUrls) {

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Phonetic(String text) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Meaning(List<Definition> definitions) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Definition(String example) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record License(String url) {
    }
}
