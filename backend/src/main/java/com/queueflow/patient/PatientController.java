package com.queueflow.patient;

import com.queueflow.common.BusinessException;
import java.time.LocalDate;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/patients")
public class PatientController {
    private final JdbcTemplate jdbc;

    public PatientController(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @GetMapping("/me")
    @PreAuthorize("hasRole('PATIENT')")
    PatientResponse current(@AuthenticationPrincipal Jwt jwt) {
        var patients = jdbc.query("""
                SELECT p.id, u.full_name, u.email, p.document, p.birth_date, p.phone, p.registration_status
                FROM patients p
                JOIN users u ON u.id = p.user_id
                WHERE p.user_id = ?
                """,
                (result, row) -> new PatientResponse(
                        UUID.fromString(result.getString("id")),
                        result.getString("full_name"),
                        result.getString("email"),
                        result.getString("document"),
                        result.getObject("birth_date", LocalDate.class),
                        result.getString("phone"),
                        result.getString("registration_status")),
                UUID.fromString(jwt.getSubject()));
        if (patients.isEmpty()) {
            throw new BusinessException("PATIENT_PROFILE_NOT_FOUND", "Cadastro de paciente não encontrado.", HttpStatus.NOT_FOUND);
        }
        return patients.getFirst();
    }

    record PatientResponse(UUID id, String fullName, String email, String document,
                           LocalDate birthDate, String phone, String registrationStatus) {}
}
