package com.lexify.dto;

public record DictionaryData(String example, String phonetic, String sourceUrl, String licenseUrl) {

    public static DictionaryData empty() {
        return new DictionaryData(null, null, null, null);
    }
}
