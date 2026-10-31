package com.lexify.controller;

import com.lexify.dto.TranslationLookupRequest;
import com.lexify.dto.TranslationResponse;
import com.lexify.service.TranslationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/translations")
@RequiredArgsConstructor
public class TranslationController {

    private final TranslationService service;

    @PostMapping
    public TranslationResponse translate(@Valid @RequestBody TranslationLookupRequest request) {
        return service.translate(request.text());
    }
}
