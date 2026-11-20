package com.lexify.controller;

import com.lexify.dto.StudyRequest;
import com.lexify.dto.StudyResponse;
import com.lexify.service.StudyService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.HttpStatus;

@RestController
@RequestMapping("/api/study")
@RequiredArgsConstructor
public class StudyController {
    private final StudyService service;

    @GetMapping
    public StudyResponse next(@AuthenticationPrincipal Jwt jwt, @RequestParam(defaultValue = "en") String direction,
                              @RequestParam(required = false) Long collectionId) {
        return service.next(Long.valueOf(jwt.getSubject()), direction, collectionId);
    }

    @PostMapping("/reviews")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void review(@AuthenticationPrincipal Jwt jwt, @Valid @RequestBody StudyRequest request) {
        service.review(Long.valueOf(jwt.getSubject()), request);
    }
}
