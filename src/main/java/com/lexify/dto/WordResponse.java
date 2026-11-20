package com.lexify.dto;
public record WordResponse(Long id, String russianText, String englishText, String example,
                           String sourceLanguage, DictionaryDetails dictionary, String russianHint) {
}
