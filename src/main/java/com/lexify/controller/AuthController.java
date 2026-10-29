package com.lexify.controller;

import com.lexify.dto.AuthRequest;
import com.lexify.dto.TokenResponse;
import com.lexify.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService service;

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public TokenResponse register(@Valid @RequestBody AuthRequest request) {
        return service.register(request);
    }

    @PostMapping("/login")
    public TokenResponse login(@Valid @RequestBody AuthRequest request) {
        return service.login(request);
    }
}
