package com.lexify.controller;

import com.lexify.dto.WordRequest;
import com.lexify.dto.WordResponse;
import com.lexify.service.WordService;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/words")
@RequiredArgsConstructor
public class WordController {

    private final WordService service;

    @GetMapping
    public List<WordResponse> list(@AuthenticationPrincipal Jwt jwt) {
        return service.list(Long.valueOf(jwt.getSubject()));
    }

    @GetMapping("/{id}")
    public WordResponse get(@AuthenticationPrincipal Jwt jwt, @PathVariable Long id) {
        return service.get(Long.valueOf(jwt.getSubject()), id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public WordResponse create(@AuthenticationPrincipal Jwt jwt, @Valid @RequestBody WordRequest request) {
        return service.create(Long.valueOf(jwt.getSubject()), request);
    }

    @PutMapping("/{id}")
    public WordResponse update(@AuthenticationPrincipal Jwt jwt, @PathVariable Long id,
                               @Valid @RequestBody WordRequest request) {
        return service.update(Long.valueOf(jwt.getSubject()), id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@AuthenticationPrincipal Jwt jwt, @PathVariable Long id) {
        service.delete(Long.valueOf(jwt.getSubject()), id);
    }

    @PostMapping("/{id}/enrich")
    public WordResponse enrich(@AuthenticationPrincipal Jwt jwt, @PathVariable Long id) {
        return service.enrich(Long.valueOf(jwt.getSubject()), id);
    }
}
