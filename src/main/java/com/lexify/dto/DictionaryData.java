package com.lexify.dto;

public record DictionaryData(String example, String phonetic, String audioUrl, String sourceUrl,
                             String licenseUrl, String audioSourceUrl, String audioLicenseUrl) {

    public static DictionaryData empty() {
        return new DictionaryData(null, null, null, null, null, null, null);
    }
}
