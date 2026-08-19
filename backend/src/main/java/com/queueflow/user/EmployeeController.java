package com.queueflow.user;

import com.queueflow.audit.AuditService;
import com.queueflow.common.BusinessException;
import com.queueflow.notification.PatientNotificationService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
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
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin/employees")
@PreAuthorize("hasAuthority('EMPLOYEE_MANAGE')")
public class EmployeeController {
    private static final List<String> STAFF_ROLES = List.of("ADMIN_USER", "DOCTOR", "PROFESSIONAL", "RECEPTIONIST", "COUNTER_ATTENDANT");
    private final JdbcTemplate jdbc;
    private final PasswordEncoder passwordEncoder;
    private final AuditService audit;
    private final PatientNotificationService notifications;

    public EmployeeController(JdbcTemplate jdbc, PasswordEncoder passwordEncoder, AuditService audit,
                              PatientNotificationService notifications) {
        this.jdbc = jdbc; this.passwordEncoder = passwordEncoder; this.audit = audit;
        this.notifications = notifications;
    }

    @GetMapping
    List<EmployeeResponse> list(@RequestParam(defaultValue="0") int page, @RequestParam(defaultValue="50") int size) {
        int safeSize = Math.min(Math.max(size, 1), 100);
        return jdbc.query("""
                SELECT u.id,u.full_name,u.email,u.active,up.phone,ep.employee_number,ep.badge_code,
                       ep.job_title,ep.hired_on,un.name unit_name,string_agg(r.code,',') roles
                FROM employee_profiles ep
                JOIN users u ON u.id=ep.user_id
                LEFT JOIN user_profiles up ON up.user_id=u.id
                LEFT JOIN units un ON un.id=ep.unit_id
                LEFT JOIN user_roles ur ON ur.user_id=u.id
                LEFT JOIN roles r ON r.id=ur.role_id
                GROUP BY u.id,up.phone,ep.employee_number,ep.badge_code,ep.job_title,ep.hired_on,un.name
                ORDER BY u.full_name LIMIT ? OFFSET ?
                """, (result, row) -> map(result), safeSize, Math.max(page, 0) * safeSize);
    }

    @GetMapping("/roles")
    List<Map<String, String>> roles(Authentication authentication) {
        boolean developer = hasRole(authentication, "ROLE_DEV_ADMIN");
        return jdbc.query("SELECT code,name FROM roles WHERE code IN ('ADMIN_USER','DOCTOR','PROFESSIONAL','RECEPTIONIST','COUNTER_ATTENDANT') AND (? OR code<>'ADMIN_USER') ORDER BY name",
                (result, row) -> Map.of("code", result.getString("code"), "name", result.getString("name")),
                developer);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Transactional
    EmployeeResponse create(@Valid @RequestBody CreateEmployee request, @AuthenticationPrincipal Jwt jwt, Authentication authentication) {
        if (!STAFF_ROLES.contains(request.roleCode()) || (request.roleCode().equals("ADMIN_USER") && !hasRole(authentication, "ROLE_DEV_ADMIN"))) {
            throw new BusinessException("INVALID_EMPLOYEE_ROLE", "Perfil de funcionário não permitido.", HttpStatus.BAD_REQUEST);
        }
        UUID actorId = UUID.fromString(jwt.getSubject());
        UUID userId = UUID.randomUUID();
        Long sequence = jdbc.queryForObject("SELECT nextval('employee_number_sequence')", Long.class);
        String employeeNumber = "FUN-%06d".formatted(sequence);
        String badgeCode = "CAT-" + employeeNumber;
        try {
            jdbc.update("INSERT INTO users(id,email,password_hash,full_name) VALUES (?,?,?,?)", userId,
                    request.email().trim().toLowerCase(), passwordEncoder.encode(request.temporaryPassword()), request.fullName().trim());
            jdbc.update("INSERT INTO user_roles(user_id,role_id) SELECT ?,id FROM roles WHERE code=?", userId, request.roleCode());
            jdbc.update("INSERT INTO user_profiles(user_id,phone) VALUES (?,?)", userId, blankToNull(request.phone()));
            jdbc.update("INSERT INTO employee_profiles(user_id,employee_number,badge_code,job_title,unit_id,hired_on,created_by) VALUES (?,?,?,?,?,?,?)",
                    userId, employeeNumber, badgeCode, request.jobTitle().trim(), request.unitId(), request.hiredOn(), actorId);
            if (request.roleCode().equals("DOCTOR") || request.roleCode().equals("PROFESSIONAL")) {
                if (request.professionalTypeId() == null) throw new BusinessException("PROFESSIONAL_TYPE_REQUIRED", "Selecione o tipo profissional.", HttpStatus.BAD_REQUEST);
                int duration = request.defaultDurationMinutes() == null ? 30 : request.defaultDurationMinutes();
                if (duration <= 0) throw new BusinessException("INVALID_DURATION", "A duração padrão deve ser positiva.", HttpStatus.BAD_REQUEST);
                UUID professionalId = UUID.randomUUID();
                jdbc.update("INSERT INTO professionals(id,user_id,professional_type_id,registration_number,default_duration_minutes,active) VALUES (?,?,?,?,?,true)",
                        professionalId, userId, request.professionalTypeId(), blankToNull(request.registrationNumber()), duration);
                if (request.specialtyId() != null) jdbc.update("INSERT INTO professional_specialties(professional_id,specialty_id) VALUES (?,?)", professionalId, request.specialtyId());
            }
        } catch (DataIntegrityViolationException exception) {
            throw new BusinessException("EMPLOYEE_ALREADY_EXISTS", "E-mail, matrícula ou crachá já cadastrado.", HttpStatus.CONFLICT);
        }
        audit.record(actorId, "EMPLOYEE_CREATE", "employee", userId, Map.of("role", request.roleCode(), "employeeNumber", employeeNumber));
        return find(userId);
    }

    @GetMapping("/{userId}/badge")
    EmployeeResponse badge(@PathVariable UUID userId) { return find(userId); }

    @PatchMapping("/{userId}/active")
    @Transactional
    void active(@PathVariable UUID userId, @RequestBody ActiveRequest request, @AuthenticationPrincipal Jwt jwt) {
        int changed = jdbc.update("UPDATE users SET active=?,updated_at=now() WHERE id=? AND EXISTS(SELECT 1 FROM employee_profiles WHERE user_id=?)",
                request.active(), userId, userId);
        if (changed == 0) throw new BusinessException("EMPLOYEE_NOT_FOUND", "Funcionário não encontrado.", HttpStatus.NOT_FOUND);
        jdbc.update("UPDATE employee_profiles SET active=?,updated_at=now() WHERE user_id=?", request.active(), userId);
        jdbc.update("UPDATE professionals SET active=? WHERE user_id=?", request.active(), userId);
        if (!request.active()) {
            jdbc.update("UPDATE schedules SET active=false WHERE professional_id=(SELECT id FROM professionals WHERE user_id=?)", userId);
            jdbc.update("""
                    UPDATE time_slots SET blocked=true
                    WHERE starts_at>now() AND schedule_id IN (
                        SELECT schedule.id FROM schedules schedule
                        JOIN professionals professional ON professional.id=schedule.professional_id
                        WHERE professional.user_id=?
                    )
                    """, userId);
        }
        int notified = request.active() ? 0 : notifications.notifyProfessionalDisabled(userId);
        audit.record(UUID.fromString(jwt.getSubject()), "EMPLOYEE_ACTIVE_CHANGE", "employee", userId,
                Map.of("active", request.active(), "patientsNotified", notified));
    }

    private EmployeeResponse find(UUID userId) {
        return jdbc.query("""
                SELECT u.id,u.full_name,u.email,u.active,up.phone,ep.employee_number,ep.badge_code,
                       ep.job_title,ep.hired_on,un.name unit_name,string_agg(r.code,',') roles
                FROM employee_profiles ep JOIN users u ON u.id=ep.user_id
                LEFT JOIN user_profiles up ON up.user_id=u.id LEFT JOIN units un ON un.id=ep.unit_id
                LEFT JOIN user_roles ur ON ur.user_id=u.id LEFT JOIN roles r ON r.id=ur.role_id
                WHERE u.id=? GROUP BY u.id,up.phone,ep.employee_number,ep.badge_code,ep.job_title,ep.hired_on,un.name
                """, (result, row) -> map(result), userId).stream().findFirst()
                .orElseThrow(() -> new BusinessException("EMPLOYEE_NOT_FOUND", "Funcionário não encontrado.", HttpStatus.NOT_FOUND));
    }

    private EmployeeResponse map(java.sql.ResultSet result) throws java.sql.SQLException {
        return new EmployeeResponse(UUID.fromString(result.getString("id")), result.getString("full_name"), result.getString("email"),
                result.getString("phone"), result.getBoolean("active"), result.getString("employee_number"), result.getString("badge_code"),
                result.getString("job_title"), result.getObject("hired_on", LocalDate.class), result.getString("unit_name"),
                result.getString("roles") == null ? List.of() : List.of(result.getString("roles").split(",")));
    }

    private boolean hasRole(Authentication authentication, String role) { return authentication.getAuthorities().stream().anyMatch(authority -> authority.getAuthority().equals(role)); }
    private String blankToNull(String value) { return value == null || value.isBlank() ? null : value.trim(); }
    public record CreateEmployee(@NotBlank @Size(max=160) String fullName, @Email @NotBlank String email,
                                 @NotBlank @Size(min=8,max=72) String temporaryPassword, @NotBlank String roleCode,
                                 @NotBlank @Size(max=120) String jobTitle,
                                 @Size(max=30) String phone, UUID unitId, LocalDate hiredOn,
                                 UUID professionalTypeId, UUID specialtyId, @Size(max=60) String registrationNumber,
                                 Integer defaultDurationMinutes) {}
    public record EmployeeResponse(UUID id, String fullName, String email, String phone, boolean active,
                                   String employeeNumber, String badgeCode, String jobTitle, LocalDate hiredOn,
                                   String unitName, List<String> roles) {}
    public record ActiveRequest(boolean active) {}
}
