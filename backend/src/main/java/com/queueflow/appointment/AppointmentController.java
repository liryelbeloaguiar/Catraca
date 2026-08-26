package com.queueflow.appointment;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/appointments")
public class AppointmentController {
    private final AppointmentService service;
    private final JdbcTemplate jdbc;

    public AppointmentController(AppointmentService service, JdbcTemplate jdbc) {
        this.service = service; this.jdbc = jdbc;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('APPOINTMENT_READ')")
    List<Map<String,Object>> list(@RequestParam(defaultValue="0") int page,
                                  @RequestParam(defaultValue="20") int size,
                                  @AuthenticationPrincipal Jwt jwt,
                                  Authentication authentication) {
        int safe = Math.min(Math.max(size, 1), 100);
        boolean patientOnly = hasAuthority(authentication, "ROLE_PATIENT");
        return jdbc.queryForList("""
                SELECT a.id,a.status,a.overbook,ts.starts_at,ts.ends_at,u.name unit_name,
                       s.name service_name,COALESCE(patient_user.full_name,a.guest_name) patient_name,
                       c.name counter_name,professional_user.full_name professional_name
                FROM appointments a
                JOIN time_slots ts ON ts.id=a.time_slot_id
                JOIN units u ON u.id=a.unit_id
                JOIN services s ON s.id=a.service_id
                LEFT JOIN patients p ON p.id=a.patient_id
                LEFT JOIN users patient_user ON patient_user.id=p.user_id
                LEFT JOIN professionals professional ON professional.id=a.professional_id
                LEFT JOIN users professional_user ON professional_user.id=professional.user_id
                LEFT JOIN counters c ON c.id=a.counter_id
                WHERE (?=false OR p.user_id=?)
                ORDER BY ts.starts_at DESC LIMIT ? OFFSET ?
                """, patientOnly, UUID.fromString(jwt.getSubject()), safe, Math.max(page, 0) * safe);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAuthority('APPOINTMENT_MANAGE')")
    Map<String,UUID> create(@Valid @RequestBody CreateRequest request,
                            @AuthenticationPrincipal Jwt jwt,
                            Authentication authentication) {
        UUID actorId = UUID.fromString(jwt.getSubject());
        boolean patientOnly = hasAuthority(authentication, "ROLE_PATIENT");
        if (patientOnly) {
            if (request.patientId() == null) throw new org.springframework.security.access.AccessDeniedException("Patient profile is required");
            Integer ownsPatient = jdbc.queryForObject("SELECT count(*) FROM patients WHERE id=? AND user_id=?", Integer.class, request.patientId(), actorId);
            if (ownsPatient == null || ownsPatient == 0) throw new org.springframework.security.access.AccessDeniedException("Patient does not belong to user");
        }
        var command = new AppointmentService.CreateAppointment(
                request.patientId(), patientOnly ? null : request.guestName(), request.professionalId(),
                request.specialtyId(), request.serviceId(), request.unitId(), request.timeSlotId(),
                request.counterId(), request.allowOverbook());
        return Map.of("id", service.create(command, actorId, hasAuthority(authentication, "APPOINTMENT_OVERBOOK")));
    }

    @PostMapping("/{id}/status")
    @PreAuthorize("hasAuthority('APPOINTMENT_MANAGE')")
    void status(@PathVariable UUID id, @Valid @RequestBody StatusRequest request,
                @AuthenticationPrincipal Jwt jwt, Authentication authentication) {
        service.changeStatus(id, request.status(), UUID.fromString(jwt.getSubject()),
                hasAuthority(authentication, "ROLE_PATIENT"));
    }

    private boolean hasAuthority(Authentication authentication, String authority) {
        return authentication.getAuthorities().stream().anyMatch(item -> item.getAuthority().equals(authority));
    }

    public record CreateRequest(UUID patientId, String guestName, UUID professionalId, UUID specialtyId,
                                @NotNull UUID serviceId, @NotNull UUID unitId, @NotNull UUID timeSlotId,
                                UUID counterId, boolean allowOverbook) {}
    public record StatusRequest(@NotNull AppointmentStatus status) {}
}
