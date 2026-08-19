package com.queueflow.configuration;

import com.queueflow.audit.AuditService;
import com.queueflow.common.BusinessException;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController @RequestMapping("/api/v1")
public class CatalogController {
    private static final Map<String, String> TABLES = Map.of(
            "units", "units", "specialties", "specialties", "services", "services",
            "priorities", "priorities", "professional-types", "professional_types", "queues", "queues",
            "rooms", "rooms", "counters", "counters", "departments", "departments");
    private final JdbcTemplate jdbc;
    private final AuditService audit;
    public CatalogController(JdbcTemplate jdbc, AuditService audit) { this.jdbc = jdbc; this.audit = audit; }

    @GetMapping("/{resource:units|specialties|services|priorities|professional-types|queues|rooms|counters|departments}")
    List<Map<String, Object>> list(@PathVariable String resource, @RequestParam(defaultValue="0") int page, @RequestParam(defaultValue="20") int size,
                                   @RequestParam(required=false) String search) {
        var table = table(resource); int safeSize = Math.min(Math.max(size, 1), 100); String term = search == null ? "" : search.trim();
        return jdbc.queryForList("SELECT * FROM " + table + " WHERE (?='' OR lower(name) LIKE lower(?)) ORDER BY name LIMIT ? OFFSET ?", term, "%"+term+"%", safeSize, Math.max(page,0)*safeSize);
    }

    @PostMapping("/{resource:units|specialties|services|priorities|professional-types|queues|rooms|counters|departments}")
    @ResponseStatus(HttpStatus.CREATED) @PreAuthorize("hasAuthority('ADMINISTRATION_MANAGE')")
    Map<String,Object> create(@PathVariable String resource, @Valid @RequestBody CatalogRequest request, @AuthenticationPrincipal Jwt jwt) {
        UUID id = UUID.randomUUID(); String table = table(resource);
        switch (resource) {
            case "units" -> jdbc.update("INSERT INTO units(id,code,name,address,active) VALUES (?,?,?,?,?)", id, requiredCode(request), request.name(), request.description(), request.active());
            case "specialties" -> jdbc.update("INSERT INTO specialties(id,code,name,description,active) VALUES (?,?,?,?,?)", id, requiredCode(request), request.name(), request.description(), request.active());
            case "professional-types" -> jdbc.update("INSERT INTO professional_types(id,code,name,active) VALUES (?,?,?,?)", id, requiredCode(request), request.name(), request.active());
            case "services" -> jdbc.update("INSERT INTO services(id,code,name,description,default_duration_minutes,requires_professional,requires_counter,active) VALUES (?,?,?,?,?,?,?,?)", id, requiredCode(request), request.name(), request.description(), positive(request.durationMinutes()), request.requiresProfessional(), request.requiresCounter(), request.active());
            case "priorities" -> jdbc.update("INSERT INTO priorities(id,name,description,weight,display_order,active) VALUES (?,?,?,?,?,?)", id, request.name(), request.description(), nonNegative(request.weight()), nonNegative(request.displayOrder()), request.active());
            case "queues" -> jdbc.update("INSERT INTO queues(id,unit_id,name,ticket_prefix,grace_period_minutes,no_show_after_minutes,automatic_reallocation_enabled,active) VALUES (?,?,?,?,?,?,?,?)", id, required(request.unitId(), "unitId"), request.name(), requiredCode(request), nonNegative(request.gracePeriodMinutes()), nonNegative(request.noShowAfterMinutes()), request.automaticReallocationEnabled(), request.active());
            case "rooms" -> jdbc.update("INSERT INTO rooms(id,unit_id,code,name,floor,active) VALUES (?,?,?,?,?,?)", id, required(request.unitId(), "unitId"), requiredCode(request), request.name(), request.description(), request.active());
            case "counters" -> jdbc.update("INSERT INTO counters(id,unit_id,code,name,active) VALUES (?,?,?,?,?)", id, required(request.unitId(), "unitId"), requiredCode(request), request.name(), request.active());
            case "departments" -> jdbc.update("INSERT INTO departments(id,unit_id,code,name,active) VALUES (?,?,?,?,?)", id, required(request.unitId(), "unitId"), requiredCode(request), request.name(), request.active());
            default -> throw notFound();
        }
        audit.record(UUID.fromString(jwt.getSubject()), "CREATE", table, id, Map.of("name", request.name()));
        return Map.of("id", id);
    }

    @PutMapping("/{resource:units|specialties|services|priorities|professional-types|queues|rooms|counters|departments}/{id}")
    @PreAuthorize("hasAuthority('ADMINISTRATION_MANAGE')")
    void update(@PathVariable String resource, @PathVariable UUID id,
                @Valid @RequestBody CatalogRequest request, @AuthenticationPrincipal Jwt jwt) {
        int changed = switch (resource) {
            case "units" -> jdbc.update("UPDATE units SET code=?,name=?,address=? WHERE id=?",
                    requiredCode(request), request.name(), request.description(), id);
            case "specialties" -> jdbc.update("UPDATE specialties SET code=?,name=?,description=? WHERE id=?",
                    requiredCode(request), request.name(), request.description(), id);
            case "professional-types" -> jdbc.update("UPDATE professional_types SET code=?,name=? WHERE id=?",
                    requiredCode(request), request.name(), id);
            case "services" -> jdbc.update("UPDATE services SET code=?,name=?,description=?,default_duration_minutes=? WHERE id=?",
                    requiredCode(request), request.name(), request.description(), positive(request.durationMinutes()), id);
            case "priorities" -> jdbc.update("UPDATE priorities SET name=?,description=?,weight=?,display_order=? WHERE id=?",
                    request.name(), request.description(), nonNegative(request.weight()), nonNegative(request.displayOrder()), id);
            case "queues" -> jdbc.update("UPDATE queues SET unit_id=?,name=?,ticket_prefix=?,grace_period_minutes=?,no_show_after_minutes=? WHERE id=?",
                    required(request.unitId(), "unitId"), request.name(), requiredCode(request),
                    nonNegative(request.gracePeriodMinutes()), nonNegative(request.noShowAfterMinutes()), id);
            case "rooms" -> jdbc.update("UPDATE rooms SET unit_id=?,code=?,name=?,floor=? WHERE id=?",
                    required(request.unitId(), "unitId"), requiredCode(request), request.name(), request.description(), id);
            case "counters" -> jdbc.update("UPDATE counters SET unit_id=?,code=?,name=? WHERE id=?",
                    required(request.unitId(), "unitId"), requiredCode(request), request.name(), id);
            case "departments" -> jdbc.update("UPDATE departments SET unit_id=?,code=?,name=? WHERE id=?",
                    required(request.unitId(), "unitId"), requiredCode(request), request.name(), id);
            default -> throw notFound();
        };
        if (changed == 0) throw notFound();
        audit.record(UUID.fromString(jwt.getSubject()), "UPDATE", table(resource), id, Map.of("name", request.name()));
    }

    @PatchMapping("/{resource:units|specialties|services|priorities|professional-types|queues|rooms|counters|departments}/{id}/active")
    @PreAuthorize("hasAuthority('ADMINISTRATION_MANAGE')")
    void active(@PathVariable String resource, @PathVariable UUID id, @RequestBody ActiveRequest request, @AuthenticationPrincipal Jwt jwt) {
        int changed = jdbc.update("UPDATE " + table(resource) + " SET active=? WHERE id=?", request.active(), id);
        if (changed == 0) throw notFound();
        audit.record(UUID.fromString(jwt.getSubject()), "CHANGE_ACTIVE", table(resource), id, Map.of("active", request.active()));
    }

    private String table(String resource) { var table = TABLES.get(resource); if (table == null) throw notFound(); return table; }
    private String requiredCode(CatalogRequest request) { if (request.code() == null || request.code().isBlank()) throw new BusinessException("VALIDATION_ERROR", "code é obrigatório.", HttpStatus.BAD_REQUEST); return request.code().trim().toUpperCase(); }
    private int positive(Integer value) { if (value == null || value <= 0) throw new BusinessException("VALIDATION_ERROR", "durationMinutes deve ser positivo.", HttpStatus.BAD_REQUEST); return value; }
    private int nonNegative(Integer value) { return value == null ? 0 : Math.max(value, 0); }
    private <T> T required(T value, String name) { if (value == null) throw new BusinessException("VALIDATION_ERROR", name+" é obrigatório.", HttpStatus.BAD_REQUEST); return value; }
    private BusinessException notFound() { return new BusinessException("RESOURCE_NOT_FOUND", "Recurso não encontrado.", HttpStatus.NOT_FOUND); }
    public record CatalogRequest(String code, @NotBlank String name, String description, Boolean active, Integer durationMinutes,
            Boolean requiresProfessional, Boolean requiresCounter, Integer weight, Integer displayOrder, UUID unitId,
            Integer gracePeriodMinutes, Integer noShowAfterMinutes, Boolean automaticReallocationEnabled) {
        public CatalogRequest { active = active == null ? true : active; requiresProfessional = requiresProfessional == null ? true : requiresProfessional; requiresCounter = requiresCounter == null ? false : requiresCounter; automaticReallocationEnabled = automaticReallocationEnabled == null ? false : automaticReallocationEnabled; }
    }
    public record ActiveRequest(boolean active) {}
}
