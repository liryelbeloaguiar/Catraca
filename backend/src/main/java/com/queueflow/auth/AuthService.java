package com.queueflow.auth;

import com.queueflow.common.BusinessException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.List;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {
    private final JdbcTemplate jdbc;
    private final PasswordEncoder passwordEncoder;
    private final TokenService tokens;
    private final long refreshDays;
    private final SecureRandom random = new SecureRandom();

    public AuthService(JdbcTemplate jdbc, PasswordEncoder passwordEncoder, TokenService tokens,
                       @Value("${app.security.refresh-token-days}") long refreshDays) {
        this.jdbc = jdbc; this.passwordEncoder = passwordEncoder; this.tokens = tokens; this.refreshDays = refreshDays;
    }

    @Transactional
    public Session login(String email, String password) {
        var users = jdbc.query("SELECT id, email, password_hash, full_name, active, locked_until FROM users WHERE lower(email)=lower(?)",
                (rs, row) -> new UserRow(UUID.fromString(rs.getString("id")), rs.getString("email"), rs.getString("password_hash"),
                        rs.getString("full_name"), rs.getBoolean("active"), rs.getTimestamp("locked_until") == null ? null : rs.getTimestamp("locked_until").toInstant()), email);
        if (users.isEmpty() || !users.getFirst().active || (users.getFirst().lockedUntil != null && users.getFirst().lockedUntil.isAfter(Instant.now())))
            throw invalidCredentials();
        var user = users.getFirst();
        if (!passwordEncoder.matches(password, user.passwordHash)) {
            jdbc.update("UPDATE users SET failed_login_attempts=failed_login_attempts+1, locked_until=CASE WHEN failed_login_attempts>=4 THEN now()+interval '15 minutes' ELSE locked_until END WHERE id=?", user.id);
            throw invalidCredentials();
        }
        jdbc.update("UPDATE users SET failed_login_attempts=0, locked_until=NULL WHERE id=?", user.id);
        return createSession(user);
    }

    @Transactional
    public Session registerPatient(RegisterPatient command) {
        UUID userId = UUID.randomUUID();
        try {
            jdbc.update("INSERT INTO users(id,email,password_hash,full_name) VALUES (?,?,?,?)", userId,
                    command.email().trim().toLowerCase(), passwordEncoder.encode(command.password()), command.fullName().trim());
            jdbc.update("INSERT INTO user_roles(user_id,role_id) SELECT ?,id FROM roles WHERE code='PATIENT'", userId);
            jdbc.update("INSERT INTO patients(user_id,document,birth_date,phone) VALUES (?,?,?,?)", userId,
                    command.document().replaceAll("[^A-Za-z0-9]", ""), command.birthDate(), command.phone());
        } catch (org.springframework.dao.DataIntegrityViolationException exception) {
            throw new BusinessException("USER_ALREADY_EXISTS", "E-mail ou documento já cadastrado.", HttpStatus.CONFLICT);
        }
        return createSession(new UserRow(userId, command.email().trim().toLowerCase(), "", command.fullName().trim(), true, null));
    }

    @Transactional
    public Session refresh(String rawToken) {
        if (rawToken == null || rawToken.isBlank()) {
            throw new BusinessException("INVALID_REFRESH_TOKEN", "Sessão expirada.", HttpStatus.UNAUTHORIZED);
        }
        var rows = jdbc.query("SELECT u.id,u.email,u.full_name FROM refresh_tokens r JOIN users u ON u.id=r.user_id WHERE r.token_hash=? AND r.revoked_at IS NULL AND r.expires_at>now() AND u.active=true FOR UPDATE",
                (rs, row) -> new UserRow(UUID.fromString(rs.getString("id")), rs.getString("email"), "", rs.getString("full_name"), true, null), hash(rawToken));
        if (rows.isEmpty()) throw new BusinessException("INVALID_REFRESH_TOKEN", "Sessão expirada.", HttpStatus.UNAUTHORIZED);
        jdbc.update("UPDATE refresh_tokens SET revoked_at=now() WHERE token_hash=?", hash(rawToken));
        return createSession(rows.getFirst());
    }

    public void logout(String rawToken) { if (rawToken != null) jdbc.update("UPDATE refresh_tokens SET revoked_at=now() WHERE token_hash=?", hash(rawToken)); }

    private Session createSession(UserRow user) {
        var authorities = jdbc.queryForList("SELECT DISTINCT 'ROLE_'||r.code FROM user_roles ur JOIN roles r ON r.id=ur.role_id WHERE ur.user_id=? UNION SELECT DISTINCT p.code FROM user_roles ur JOIN role_permissions rp ON rp.role_id=ur.role_id JOIN permissions p ON p.id=rp.permission_id WHERE ur.user_id=?", String.class, user.id, user.id);
        var rawRefresh = randomToken();
        jdbc.update("INSERT INTO refresh_tokens(user_id,token_hash,expires_at) VALUES (?,?,?)", user.id, hash(rawRefresh),
                java.sql.Timestamp.from(Instant.now().plus(refreshDays, ChronoUnit.DAYS)));
        return new Session(tokens.accessToken(user.id, user.email, authorities), rawRefresh, tokens.accessDuration().toSeconds(), user.fullName, authorities);
    }

    private String randomToken() { var bytes = new byte[48]; random.nextBytes(bytes); return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes); }
    private String hash(String value) { try { return java.util.HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8))); } catch (Exception e) { throw new IllegalStateException(e); } }
    private BusinessException invalidCredentials() { return new BusinessException("INVALID_CREDENTIALS", "Credenciais inválidas.", HttpStatus.UNAUTHORIZED); }
    private record UserRow(UUID id, String email, String passwordHash, String fullName, boolean active, Instant lockedUntil) {}
    public record Session(String accessToken, String refreshToken, long expiresIn, String fullName, List<String> authorities) {}
}
