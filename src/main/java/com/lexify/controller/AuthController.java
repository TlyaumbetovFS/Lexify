package com.lexify.controller;

import com.lexify.dto.AuthRequest;
import com.lexify.dto.AuthResponse;
import com.lexify.service.AuthService;
import com.lexify.service.RefreshSessionService;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import java.time.Duration;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@SecurityRequirements
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private static final String REFRESH_COOKIE = "lexify_refresh";
    private final AuthService service;
    private final RefreshSessionService sessions;

    @Value("${app.auth.secure-cookie}")
    private boolean secureCookie;

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public AuthResponse register(@Valid @RequestBody AuthRequest request,
                                 @CookieValue(name = REFRESH_COOKIE, required = false) String previous,
                                 HttpServletResponse response) {
        return start(sessions.create(service.register(request), previous), response);
    }

    @PostMapping("/login")
    public AuthResponse login(@Valid @RequestBody AuthRequest request,
                              @CookieValue(name = REFRESH_COOKIE, required = false) String previous,
                              HttpServletResponse response) {
        return start(sessions.create(service.login(request), previous), response);
    }

    @PostMapping("/refresh")
    public AuthResponse refresh(@CookieValue(name = REFRESH_COOKIE, required = false) String token,
                                @RequestHeader("X-Lexify-Session") String sessionHeader,
                                HttpServletResponse response) {
        response.setHeader(HttpHeaders.CACHE_CONTROL, "no-store");
        return sessions.refresh(token);
    }

    @PostMapping("/logout")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void logout(@CookieValue(name = REFRESH_COOKIE, required = false) String token,
                       @RequestHeader("X-Lexify-Session") String sessionHeader,
                       HttpServletResponse response) {
        sessions.revoke(token);
        setCookie(response, "", Duration.ZERO);
    }

    private AuthResponse start(RefreshSessionService.Login login, HttpServletResponse response) {
        setCookie(response, login.refreshToken(), RefreshSessionService.LIFETIME);
        return login.response();
    }

    private void setCookie(HttpServletResponse response, String value, Duration maxAge) {
        var cookie = ResponseCookie.from(REFRESH_COOKIE, value)
                .httpOnly(true)
                .secure(secureCookie)
                .sameSite("Strict")
                .path("/api/auth")
                .maxAge(maxAge)
                .build();
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
        response.setHeader(HttpHeaders.CACHE_CONTROL, "no-store");
    }
}
