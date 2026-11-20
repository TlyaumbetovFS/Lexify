package com.lexify.dto;

import java.util.List;

public record AnkiPreview(List<NoteType> noteTypes, int totalNotes, int unsupportedNotes) {
    public record NoteType(long id, String name, List<String> fields, int notes, List<List<String>> samples) {}
}
