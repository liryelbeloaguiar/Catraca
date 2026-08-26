package com.queueflow.user;

import com.queueflow.audit.AuditService;
import com.queueflow.common.BusinessException;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.List;
import java.util.UUID;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/v1/profile")
public class ProfileController {
    private final JdbcTemplate jdbc;
    private final PasswordEncoder passwordEncoder;
    private final AuditService audit;
    private final AvatarImageService avatarImages;

    public ProfileController(JdbcTemplate jdbc, PasswordEncoder passwordEncoder, AuditService audit,
                             AvatarImageService avatarImages) {
        this.jdbc = jdbc; this.passwordEncoder = passwordEncoder; this.audit = audit;
        this.avatarImages = avatarImages;
    }

    @GetMapping
    ProfileResponse current(@AuthenticationPrincipal Jwt jwt) {
        UUID userId = UUID.fromString(jwt.getSubject());
        return jdbc.query("""
                SELECT u.id,u.full_name,u.email,u.active,up.phone,
                       (up.avatar_data IS NOT NULL) has_avatar,
                       ep.employee_number,ep.badge_code,ep.job_title,ep.hired_on,
                       COALESCE(array_agg(DISTINCT r.code) FILTER (WHERE r.code IS NOT NULL), '{}') roles
                FROM users u
                LEFT JOIN user_profiles up ON up.user_id=u.id
                LEFT JOIN employee_profiles ep ON ep.user_id=u.id
                LEFT JOIN user_roles ur ON ur.user_id=u.id
                LEFT JOIN roles r ON r.id=ur.role_id
                WHERE u.id=?
                GROUP BY u.id,up.phone,up.avatar_data,ep.employee_number,ep.badge_code,ep.job_title,ep.hired_on
                """, (result, row) -> new ProfileResponse(
                        UUID.fromString(result.getString("id")), result.getString("full_name"), result.getString("email"),
                        result.getString("phone"), result.getBoolean("active"), result.getBoolean("has_avatar"),
                        result.getString("employee_number"), result.getString("badge_code"), result.getString("job_title"),
                        result.getObject("hired_on", java.time.LocalDate.class), List.of((String[]) result.getArray("roles").getArray())),
                userId).stream().findFirst().orElseThrow(() -> notFound("Usuário não encontrado."));
    }

    @PatchMapping
    @Transactional
    ProfileResponse update(@Valid @RequestBody UpdateProfile request, @AuthenticationPrincipal Jwt jwt) {
        UUID userId = UUID.fromString(jwt.getSubject());
        jdbc.update("UPDATE users SET full_name=?,updated_at=now() WHERE id=?", request.fullName().trim(), userId);
        jdbc.update("""
                INSERT INTO user_profiles(user_id,phone) VALUES (?,?)
                ON CONFLICT(user_id) DO UPDATE SET phone=EXCLUDED.phone,updated_at=now()
                """, userId, blankToNull(request.phone()));
        audit.record(userId, "PROFILE_UPDATE", "user", userId, java.util.Map.of("fullName", request.fullName().trim()));
        return current(jwt);
    }

    @PostMapping("/password")
    @Transactional
    void changePassword(@Valid @RequestBody ChangePassword request, @AuthenticationPrincipal Jwt jwt) {
        UUID userId = UUID.fromString(jwt.getSubject());
        String currentHash = jdbc.queryForObject("SELECT password_hash FROM users WHERE id=?", String.class, userId);
        if (currentHash == null || !passwordEncoder.matches(request.currentPassword(), currentHash)) {
            throw new BusinessException("INVALID_CURRENT_PASSWORD", "A senha atual está incorreta.", HttpStatus.BAD_REQUEST);
        }
        if (request.currentPassword().equals(request.newPassword())) {
            throw new BusinessException("PASSWORD_NOT_CHANGED", "A nova senha deve ser diferente da senha atual.", HttpStatus.BAD_REQUEST);
        }
        jdbc.update("UPDATE users SET password_hash=?,failed_login_attempts=0,locked_until=NULL,updated_at=now() WHERE id=?",
                passwordEncoder.encode(request.newPassword()), userId);
        jdbc.update("UPDATE refresh_tokens SET revoked_at=now() WHERE user_id=? AND revoked_at IS NULL", userId);
        audit.record(userId, "PASSWORD_CHANGE", "user", userId, java.util.Map.of());
    }

    @PostMapping(value = "/avatar", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Transactional
    void avatar(@RequestParam("file") MultipartFile file, @AuthenticationPrincipal Jwt jwt) {
        var processed = avatarImages.process(file);
        UUID userId = UUID.fromString(jwt.getSubject());
        jdbc.update("""
                INSERT INTO user_profiles(user_id,avatar_data,avatar_content_type) VALUES (?,?,?)
                ON CONFLICT(user_id) DO UPDATE SET avatar_data=EXCLUDED.avatar_data,
                    avatar_content_type=EXCLUDED.avatar_content_type,updated_at=now()
                """, userId, processed.content(), processed.contentType());
        audit.record(userId, "AVATAR_UPDATE", "user", userId,
                java.util.Map.of("contentType", processed.contentType(), "size", processed.content().length));
    }

    @GetMapping("/avatar/{userId}")
    @PreAuthorize("#userId.toString() == authentication.name or hasAuthority('EMPLOYEE_MANAGE')")
    ResponseEntity<byte[]> avatar(@PathVariable UUID userId) {
        var images = jdbc.query("SELECT avatar_data,avatar_content_type FROM user_profiles WHERE user_id=? AND avatar_data IS NOT NULL",
                (result, row) -> new Avatar(result.getBytes("avatar_data"), result.getString("avatar_content_type")), userId);
        if (images.isEmpty()) return ResponseEntity.notFound().build();
        var image = images.getFirst();
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).header(HttpHeaders.CONTENT_TYPE, image.contentType()).body(image.content());
    }

    private String blankToNull(String value) { return value == null || value.isBlank() ? null : value.trim(); }
    private BusinessException notFound(String message) { return new BusinessException("RESOURCE_NOT_FOUND", message, HttpStatus.NOT_FOUND); }
    public record UpdateProfile(@NotBlank @Size(max=160) String fullName, @Size(max=30) String phone) {}
    public record ChangePassword(@NotBlank String currentPassword, @NotBlank @Size(min=8,max=72) String newPassword) {}
    public record ProfileResponse(UUID id, String fullName, String email, String phone, boolean active, boolean hasAvatar,
                                  String employeeNumber, String badgeCode, String jobTitle, java.time.LocalDate hiredOn, List<String> roles) {}
    private record Avatar(byte[] content, String contentType) {}
}
