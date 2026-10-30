package com.lexify.controller;

import com.lexify.dto.RoomRequest;
import com.lexify.dto.RoomResponse;
import com.lexify.service.RoomService;
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
@RequestMapping("/api/rooms")
@RequiredArgsConstructor
public class RoomController {

    private final RoomService service;

    @GetMapping
    public List<RoomResponse> list() {
        return service.list();
    }

    @GetMapping("/{id}")
    public RoomResponse get(@PathVariable Long id) {
        return service.get(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public RoomResponse create(@AuthenticationPrincipal Jwt jwt, @Valid @RequestBody RoomRequest request) {
        return service.create(Long.valueOf(jwt.getSubject()), request);
    }

    @PutMapping("/{id}")
    public RoomResponse update(@AuthenticationPrincipal Jwt jwt, @PathVariable Long id,
                               @Valid @RequestBody RoomRequest request) {
        return service.update(Long.valueOf(jwt.getSubject()), id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@AuthenticationPrincipal Jwt jwt, @PathVariable Long id) {
        service.delete(Long.valueOf(jwt.getSubject()), id);
    }

    @PutMapping("/{id}/moderators/{userId}")
    public RoomResponse addModerator(@AuthenticationPrincipal Jwt jwt, @PathVariable Long id,
                                     @PathVariable Long userId) {
        return service.addModerator(Long.valueOf(jwt.getSubject()), id, userId);
    }

    @DeleteMapping("/{id}/moderators/{userId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void removeModerator(@AuthenticationPrincipal Jwt jwt, @PathVariable Long id, @PathVariable Long userId) {
        service.removeModerator(Long.valueOf(jwt.getSubject()), id, userId);
    }
}
