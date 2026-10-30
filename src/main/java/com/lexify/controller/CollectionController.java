package com.lexify.controller;

import com.lexify.dto.CollectionRequest;
import com.lexify.dto.CollectionResponse;
import com.lexify.service.CollectionService;
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
@RequestMapping("/api/collections")
@RequiredArgsConstructor
public class CollectionController {

    private final CollectionService service;

    @GetMapping
    public List<CollectionResponse> list(@AuthenticationPrincipal Jwt jwt) {
        return service.list(Long.valueOf(jwt.getSubject()));
    }

    @GetMapping("/{id}")
    public CollectionResponse get(@AuthenticationPrincipal Jwt jwt, @PathVariable Long id) {
        return service.get(Long.valueOf(jwt.getSubject()), id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CollectionResponse create(@AuthenticationPrincipal Jwt jwt, @Valid @RequestBody CollectionRequest request) {
        return service.create(Long.valueOf(jwt.getSubject()), request);
    }

    @PutMapping("/{id}")
    public CollectionResponse update(@AuthenticationPrincipal Jwt jwt, @PathVariable Long id,
                                     @Valid @RequestBody CollectionRequest request) {
        return service.update(Long.valueOf(jwt.getSubject()), id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@AuthenticationPrincipal Jwt jwt, @PathVariable Long id) {
        service.delete(Long.valueOf(jwt.getSubject()), id);
    }

    @PutMapping("/{id}/words/{wordId}")
    public CollectionResponse addWord(@AuthenticationPrincipal Jwt jwt, @PathVariable Long id,
                                      @PathVariable Long wordId) {
        return service.addWord(Long.valueOf(jwt.getSubject()), id, wordId);
    }

    @DeleteMapping("/{id}/words/{wordId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void removeWord(@AuthenticationPrincipal Jwt jwt, @PathVariable Long id, @PathVariable Long wordId) {
        service.removeWord(Long.valueOf(jwt.getSubject()), id, wordId);
    }
}
