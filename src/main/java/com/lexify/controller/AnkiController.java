package com.lexify.controller;

import com.lexify.dto.AnkiImportResult;
import com.lexify.dto.AnkiPreview;
import com.lexify.service.AnkiImportService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/anki")
@RequiredArgsConstructor
public class AnkiController {
    private final AnkiImportService service;

    @PostMapping(value = "/preview", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public AnkiPreview preview(@RequestParam MultipartFile file) {
        return service.preview(file);
    }

    @PostMapping(value = "/import", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public AnkiImportResult importWords(@AuthenticationPrincipal Jwt jwt, @RequestParam MultipartFile file,
            @RequestParam long modelId, @RequestParam int englishField, @RequestParam int russianField,
            @RequestParam(required = false) Integer exampleField,
            @RequestParam(required = false) Integer hintField, @RequestParam String name) {
        return service.importWords(Long.valueOf(jwt.getSubject()), file, modelId, englishField, russianField,
                exampleField, hintField, name);
    }
}
