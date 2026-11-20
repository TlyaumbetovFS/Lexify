package com.lexify.dto;

import java.util.List;

public record AnkiImportResult(long collectionId, int created, int reused, int skipped, List<String> warnings) {}
