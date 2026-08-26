package com.queueflow.user;

import com.queueflow.audit.AuditService;
import com.queueflow.common.BusinessException;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.access.prepost.PreAuthorize;
import com.queueflow.security.SecurityExpressions;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/dev/users")
@PreAuthorize(SecurityExpressions.DEV_ADMIN)
public class UserAdministrationController {
    private final JdbcTemplate jdbc;
    private final AuditService audit;

    public UserAdministrationController(JdbcTemplate jdbc, AuditService audit) {
        this.jdbc = jdbc;
        this.audit = audit;
    }

    @GetMapping
    List<UserResponse> list(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size,
            @RequestParam(defaultValue = "") String search) {
        int safeSize = Math.min(Math.max(size, 1), 100);
        String term = "%" + search.trim() + "%";
        return jdbc.query("""
                SELECT u.id, u.full_name, u.email, u.active, u.created_at,
                       up.phone, (up.avatar_data IS NOT NULL) has_avatar,
                       ep.employee_number,
                       COALESCE(string_agg(DISTINCT r.code, ',' ORDER BY r.code), '') roles
                FROM users u
                LEFT JOIN user_profiles up ON up.user_id=u.id
                LEFT JOIN employee_profiles ep ON ep.user_id=u.id
                LEFT JOIN user_roles ur ON ur.user_id=u.id
                LEFT JOIN roles r ON r.id=ur.role_id
                WHERE (?='' OR u.full_name ILIKE ? OR u.email ILIKE ?)
                GROUP BY u.id, up.phone, up.avatar_data, ep.employee_number
                ORDER BY u.full_name
                LIMIT ? OFFSET ?
                """, (result, row) -> new UserResponse(
                        UUID.fromString(result.getString("id")),
                        result.getString("full_name"),
                        result.getString("email"),
                        result.getString("phone"),
                        result.getBoolean("active"),
                        result.getBoolean("has_avatar"),
                        result.getString("employee_number"),
                        result.getTimestamp("created_at").toInstant(),
                        result.getString("roles").isBlank()
                                ? List.of()
                                : List.of(result.getString("roles").split(","))),
                search.trim(), term, term, safeSize, Math.max(page, 0) * safeSize);
    }

    @PatchMapping("/{userId}/active")
    @Transactional
    void active(
            @PathVariable UUID userId,
            @RequestBody ActiveRequest request,
            @AuthenticationPrincipal Jwt jwt) {
        UUID actorId = UUID.fromString(jwt.getSubject());
        if (actorId.equals(userId) && !request.active()) {
            throw new BusinessException(
                    "SELF_DEACTIVATION_NOT_ALLOWED",
                    "O administrador técnico não pode desativar o próprio acesso.",
                    HttpStatus.CONFLICT);
        }
        int changed = jdbc.update(
                "UPDATE users SET active=?, updated_at=now() WHERE id=?",
                request.active(), userId);
        if (changed == 0) {
            throw new BusinessException(
                    "USER_NOT_FOUND", "Usuário não encontrado.", HttpStatus.NOT_FOUND);
        }
        if (!request.active()) {
            jdbc.update(
                    "UPDATE refresh_tokens SET revoked_at=now() WHERE user_id=? AND revoked_at IS NULL",
                    userId);
        }
        audit.record(actorId, "USER_ACTIVE_CHANGE", "user", userId,
                java.util.Map.of("active", request.active()));
    }

    public record ActiveRequest(boolean active) {}

    public record UserResponse(
            UUID id,
            String fullName,
            String email,
            String phone,
            boolean active,
            boolean hasAvatar,
            String employeeNumber,
            java.time.Instant createdAt,
            List<String> roles) {}
}
