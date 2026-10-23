package com.tronai.auth;

import com.tronai.auth.dto.AuthResponse;
import com.tronai.auth.dto.LoginRequest;
import com.tronai.auth.dto.RegisterRequest;
import com.tronai.auth.dto.UserDto;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/auth")
@Tag(name = "Authentication", description = "User registration, authentication, token refresh, and identity endpoints")
public class AuthController {

    private final AuthService authService;
    private final com.tronai.auth.identity.GoogleIdentityService googleIdentityService;

    public AuthController(AuthService authService,
                          com.tronai.auth.identity.GoogleIdentityService googleIdentityService) {
        this.authService = authService;
        this.googleIdentityService = googleIdentityService;
    }

    @GetMapping("/providers")
    @Operation(summary = "Get configured authentication providers")
    public ResponseEntity<Map<String, Object>> getProviders() {
        return ResponseEntity.ok(Map.of(
                "google", Map.of("enabled", googleIdentityService.isGoogleAuthEnabled()),
                "password", Map.of("enabled", true)
        ));
    }

    @PostMapping("/register")
    @Operation(summary = "Register a new user")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest request) {
        AuthService.AuthResult result = authService.register(request);
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, result.cookie().toString())
                .body(result.response());
    }

    @PostMapping("/login")
    @Operation(summary = "Authenticate user and issue JWT access token + refresh cookie")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        AuthService.AuthResult result = authService.login(request);
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, result.cookie().toString())
                .body(result.response());
    }

    @PostMapping("/refresh")
    @Operation(summary = "Refresh JWT access token using HttpOnly refresh cookie")
    public ResponseEntity<AuthResponse> refresh(
            @CookieValue(name = AuthService.REFRESH_COOKIE_NAME, required = false) String refreshTokenFromCookie,
            @RequestBody(required = false) Map<String, String> body) {

        String token = refreshTokenFromCookie;
        if ((token == null || token.isBlank()) && body != null && body.containsKey("refreshToken")) {
            token = body.get("refreshToken");
        }

        AuthService.AuthResult result = authService.refresh(token);
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, result.cookie().toString())
                .body(result.response());
    }

    @PostMapping("/logout")
    @Operation(summary = "Revoke refresh token and clear refresh cookie")
    public ResponseEntity<Map<String, String>> logout(
            @CookieValue(name = AuthService.REFRESH_COOKIE_NAME, required = false) String refreshTokenFromCookie,
            @RequestParam(value = "refreshToken", required = false) String refreshTokenParam) {

        // Idempotent logout. Prefer HttpOnly cookie (web). Optional query/form param for
        // non-browser clients. No @RequestBody — empty / form-urlencoded POSTs must not 415/500.
        String token = refreshTokenFromCookie;
        if ((token == null || token.isBlank()) && refreshTokenParam != null && !refreshTokenParam.isBlank()) {
            token = refreshTokenParam;
        }

        var clearCookie = authService.logout(token);
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, clearCookie.toString())
                .body(Map.of("detail", "Logged out successfully"));
    }

    @GetMapping("/me")
    @Operation(summary = "Get currently authenticated user identity")
    public ResponseEntity<UserDto> me() {
        UserPrincipal principal = SecurityUtils.getRequiredCurrentUser();
        return ResponseEntity.ok(authService.getMe(principal));
    }
}
