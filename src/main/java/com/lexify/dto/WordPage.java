package com.lexify.dto;

import java.util.List;

public record WordPage(List<WordResponse> items, long total, int pages) {}
