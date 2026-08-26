package com.queueflow.auth;

import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.http.ResponseCookie;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {
    private final AuthService auth;
    private final boolean secureCookie;
    private final long refreshTokenSeconds;
    public AuthController(AuthService auth,
                          @Value("${app.security.cookie-secure:false}") boolean secureCookie,
                          @Value("${app.security.refresh-token-days}") long refreshTokenDays) {
        this.auth = auth;
        this.secureCookie = secureCookie;
        this.refreshTokenSeconds = Math.multiplyExact(refreshTokenDays, 86400L);
    }

    @PostMapping("/login") ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request, HttpServletResponse response) {
        return respond(auth.login(request.email(), request.password()), response);
    }
    @PostMapping("/register") ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterPatient request, HttpServletResponse response) {
        return respond(auth.registerPatient(request), response);
    }
    @PostMapping("/refresh") ResponseEntity<AuthResponse> refresh(@CookieValue(value="refresh_token", required=false) String refresh, HttpServletResponse response) {
        return respond(auth.refresh(refresh), response);
    }
    @PostMapping("/logout") ResponseEntity<Void> logout(@CookieValue(value="refresh_token", required=false) String refresh, HttpServletResponse response) {
        auth.logout(refresh); response.addHeader(HttpHeaders.SET_COOKIE, cookie("", 0).toString()); return ResponseEntity.noContent().build();
    }
    private ResponseEntity<AuthResponse> respond(AuthService.Session session, HttpServletResponse response) {
        response.addHeader(HttpHeaders.SET_COOKIE, cookie(session.refreshToken(), refreshTokenSeconds).toString());
        return ResponseEntity.ok(new AuthResponse(session.accessToken(), session.expiresIn(), session.fullName(), session.authorities()));
    }
    private ResponseCookie cookie(String value, long age) { return ResponseCookie.from("refresh_token", value).httpOnly(true).secure(secureCookie).sameSite("Strict").path("/api/v1/auth").maxAge(age).build(); }
    public record LoginRequest(@Email @NotBlank String email, @NotBlank String password) {}
    public record AuthResponse(String accessToken, long expiresIn, String fullName, java.util.List<String> authorities) {}
}
