package com.queueflow.schedule;

import com.queueflow.audit.AuditService;
import com.queueflow.common.BusinessException;
import com.queueflow.notification.PatientNotificationService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/schedules")
@PreAuthorize("hasAuthority('ADMINISTRATION_MANAGE')")
public class ScheduleController {
    private final JdbcTemplate jdbc;
    private final AuditService audit;
    private final ZoneId zoneId;
    private final PatientNotificationService notifications;

    public ScheduleController(JdbcTemplate jdbc, AuditService audit,
                              @Value("${app.scheduling.zone-id:America/Sao_Paulo}") String zoneId,
                              PatientNotificationService notifications) {
        this.jdbc = jdbc; this.audit = audit; this.zoneId = ZoneId.of(zoneId);
        this.notifications = notifications;
    }

    @GetMapping
    List<Map<String,Object>> list(@RequestParam(defaultValue="0") int page, @RequestParam(defaultValue="50") int size) {
        int safeSize = Math.min(Math.max(size, 1), 100);
        return jdbc.queryForList("""
                SELECT sc.id,sc.professional_id,sc.unit_id,sc.room_id,
                       u.full_name professional_name,un.name unit_name,r.name room_name,
                       sc.valid_from,sc.valid_until,sc.active,
                       (SELECT string_agg(
                           (CASE shift.day_of_week
                               WHEN 1 THEN 'Seg' WHEN 2 THEN 'Ter' WHEN 3 THEN 'Qua'
                               WHEN 4 THEN 'Qui' WHEN 5 THEN 'Sex' WHEN 6 THEN 'Sáb'
                               WHEN 7 THEN 'Dom' END) || ' ' ||
                           to_char(shift.start_time,'HH24:MI') || '–' || to_char(shift.end_time,'HH24:MI'),
                           ' · ' ORDER BY shift.day_of_week)
                        FROM shifts shift WHERE shift.schedule_id=sc.id) shifts,
                       (SELECT min(shift.slot_duration_minutes) FROM shifts shift WHERE shift.schedule_id=sc.id) slot_duration_minutes,
                       (SELECT min(shift.default_capacity) FROM shifts shift WHERE shift.schedule_id=sc.id) capacity,
                       (SELECT min(shift.start_time) FROM shifts shift WHERE shift.schedule_id=sc.id) start_time,
                       (SELECT min(shift.end_time) FROM shifts shift WHERE shift.schedule_id=sc.id) end_time,
                       (SELECT min(shift.break_start) FROM shifts shift WHERE shift.schedule_id=sc.id) break_start,
                       (SELECT min(shift.break_end) FROM shifts shift WHERE shift.schedule_id=sc.id) break_end,
                       (SELECT string_agg(shift.day_of_week::text, ',' ORDER BY shift.day_of_week)
                        FROM shifts shift WHERE shift.schedule_id=sc.id) days_of_week,
                       (SELECT count(*) FROM time_slots slot WHERE slot.schedule_id=sc.id) slots_count
                FROM schedules sc JOIN professionals p ON p.id=sc.professional_id JOIN users u ON u.id=p.user_id
                JOIN units un ON un.id=sc.unit_id LEFT JOIN rooms r ON r.id=sc.room_id
                ORDER BY sc.valid_from DESC LIMIT ? OFFSET ?
                """, safeSize, Math.max(page, 0) * safeSize);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Transactional
    Map<String,Object> create(@Valid @RequestBody CreateSchedule request, @AuthenticationPrincipal Jwt jwt) {
        validate(request);
        UUID scheduleId = UUID.randomUUID();
        jdbc.update("INSERT INTO schedules(id,professional_id,unit_id,room_id,valid_from,valid_until,active) VALUES (?,?,?,?,?,?,true)",
                scheduleId, request.professionalId(), request.unitId(), request.roomId(), request.validFrom(), request.validUntil());
        insertShifts(scheduleId, request);
        int slotsCreated = generateSlots(scheduleId, request.professionalId(), request, request.validFrom(), null);
        UUID actorId = UUID.fromString(jwt.getSubject());
        audit.record(actorId, "SCHEDULE_CREATE", "schedule", scheduleId, Map.of("slotsCreated", slotsCreated, "zoneId", zoneId.getId()));
        return Map.of("id", scheduleId, "slotsCreated", slotsCreated);
    }

    @PutMapping("/{id}")
    @Transactional
    Map<String,Object> update(@PathVariable UUID id, @Valid @RequestBody UpdateSchedule request,
                              @AuthenticationPrincipal Jwt jwt) {
        CreateSchedule values = new CreateSchedule(request.professionalId(), request.unitId(), request.roomId(),
                request.validFrom(), request.validUntil(), request.daysOfWeek(), request.startTime(), request.endTime(),
                request.breakStart(), request.breakEnd(), request.slotDurationMinutes(), request.capacity());
        validate(values);
        var existing = jdbc.query("SELECT professional_id,unit_id FROM schedules WHERE id=?",
                (result, row) -> new ScheduleIdentity(
                        UUID.fromString(result.getString("professional_id")),
                        UUID.fromString(result.getString("unit_id"))), id).stream().findFirst()
                .orElseThrow(() -> new BusinessException("SCHEDULE_NOT_FOUND", "Escala não encontrada.", HttpStatus.NOT_FOUND));
        if (!existing.professionalId().equals(request.professionalId()) || !existing.unitId().equals(request.unitId())) {
            throw new BusinessException("SCHEDULE_OWNER_IMMUTABLE",
                    "Profissional e unidade não podem ser trocados. Crie outra escala para essa alteração.", HttpStatus.CONFLICT);
        }
        Instant now = Instant.now();
        jdbc.update("""
                DELETE FROM time_slots slot
                WHERE slot.schedule_id=? AND slot.starts_at>=? AND slot.booked_count=0
                  AND NOT EXISTS(SELECT 1 FROM appointments appointment WHERE appointment.time_slot_id=slot.id)
                """, id, Timestamp.from(now));
        jdbc.update("DELETE FROM shifts WHERE schedule_id=?", id);
        jdbc.update("UPDATE schedules SET room_id=?,valid_from=?,valid_until=? WHERE id=?",
                request.roomId(), request.validFrom(), request.validUntil(), id);
        insertShifts(id, values);
        LocalDate firstDate = request.validFrom().isAfter(LocalDate.now(zoneId))
                ? request.validFrom() : LocalDate.now(zoneId);
        int slotsCreated = generateSlots(id, existing.professionalId(), values, firstDate, now);
        int notified = notifications.notifyScheduleChange(id, "A escala ou o horário foi alterado");
        audit.record(UUID.fromString(jwt.getSubject()), "SCHEDULE_UPDATE", "schedule", id,
                Map.of("slotsCreated", slotsCreated, "patientsNotified", notified, "zoneId", zoneId.getId()));
        return Map.of("id", id, "slotsCreated", slotsCreated, "patientsNotified", notified);
    }

    @PatchMapping("/{id}/active")
    @Transactional
    void active(@PathVariable UUID id, @RequestBody ActiveRequest request, @AuthenticationPrincipal Jwt jwt) {
        int changed = jdbc.update("UPDATE schedules SET active=? WHERE id=?", request.active(), id);
        if (changed == 0) throw new BusinessException("SCHEDULE_NOT_FOUND", "Escala não encontrada.", HttpStatus.NOT_FOUND);
        jdbc.update("UPDATE time_slots SET blocked=? WHERE schedule_id=? AND starts_at>now()", !request.active(), id);
        int notified = request.active() ? 0
                : notifications.notifyScheduleChange(id, "A escala do profissional foi desativada");
        audit.record(UUID.fromString(jwt.getSubject()), "SCHEDULE_ACTIVE_CHANGE", "schedule", id,
                Map.of("active", request.active(), "patientsNotified", notified));
    }

    @PatchMapping("/time-slots/{id}/capacity")
    @Transactional
    void capacity(@PathVariable UUID id, @RequestBody CapacityRequest request, @AuthenticationPrincipal Jwt jwt) {
        int changed = jdbc.update("UPDATE time_slots SET capacity=?,version=version+1 WHERE id=? AND booked_count<=?", request.capacity(), id, request.capacity());
        if (changed == 0) throw new BusinessException("INVALID_SLOT_CAPACITY", "A capacidade não pode ser menor que os agendamentos existentes.", HttpStatus.CONFLICT);
        audit.record(UUID.fromString(jwt.getSubject()), "TIME_SLOT_CAPACITY_CHANGE", "time_slot", id, Map.of("capacity", request.capacity()));
    }

    private void validate(CreateSchedule request) {
        if (request.validUntil().isBefore(request.validFrom()) || request.validUntil().isAfter(request.validFrom().plusYears(1)))
            throw new BusinessException("INVALID_SCHEDULE_PERIOD", "O período da escala deve ter no máximo um ano.", HttpStatus.BAD_REQUEST);
        if (!request.endTime().isAfter(request.startTime())) throw new BusinessException("INVALID_SCHEDULE_TIME", "O horário final deve ser posterior ao inicial.", HttpStatus.BAD_REQUEST);
        if ((request.breakStart() == null) != (request.breakEnd() == null) || (request.breakStart() != null && !request.breakEnd().isAfter(request.breakStart())))
            throw new BusinessException("INVALID_BREAK", "Informe um intervalo válido.", HttpStatus.BAD_REQUEST);
        if (request.daysOfWeek().stream().anyMatch(day -> day < 1 || day > 7)) throw new BusinessException("INVALID_WEEK_DAY", "Dia da semana inválido.", HttpStatus.BAD_REQUEST);
    }

    private boolean overlapsBreak(LocalTime start, LocalTime end, LocalTime breakStart, LocalTime breakEnd) {
        return breakStart != null && start.isBefore(breakEnd) && end.isAfter(breakStart);
    }

    private void insertShifts(UUID scheduleId, CreateSchedule request) {
        for (int day : request.daysOfWeek()) {
            jdbc.update("INSERT INTO shifts(schedule_id,day_of_week,start_time,end_time,break_start,break_end,slot_duration_minutes,default_capacity) VALUES (?,?,?,?,?,?,?,?)",
                    scheduleId, day, request.startTime(), request.endTime(), request.breakStart(), request.breakEnd(), request.slotDurationMinutes(), request.capacity());
        }
    }

    private int generateSlots(UUID scheduleId, UUID professionalId, CreateSchedule request,
                              LocalDate firstDate, Instant notBefore) {
        int slotsCreated = 0;
        for (LocalDate date = firstDate; !date.isAfter(request.validUntil()); date = date.plusDays(1)) {
            if (!request.daysOfWeek().contains(date.getDayOfWeek().getValue())) continue;
            for (LocalTime start = request.startTime(); !start.plusMinutes(request.slotDurationMinutes()).isAfter(request.endTime()); start = start.plusMinutes(request.slotDurationMinutes())) {
                LocalTime end = start.plusMinutes(request.slotDurationMinutes());
                if (overlapsBreak(start, end, request.breakStart(), request.breakEnd())) continue;
                var startsAt = date.atTime(start).atZone(zoneId).toInstant();
                var endsAt = date.atTime(end).atZone(zoneId).toInstant();
                if (notBefore != null && startsAt.isBefore(notBefore)) continue;
                Integer existingSlot = jdbc.queryForObject(
                        "SELECT count(*) FROM time_slots WHERE schedule_id=? AND starts_at=?",
                        Integer.class, scheduleId, Timestamp.from(startsAt));
                if (existingSlot != null && existingSlot > 0) continue;
                Integer conflicts = jdbc.queryForObject("""
                        SELECT count(*) FROM time_slots ts JOIN schedules sc ON sc.id=ts.schedule_id
                        WHERE sc.professional_id=? AND ts.starts_at<? AND ts.ends_at>?
                        """, Integer.class, professionalId, Timestamp.from(endsAt), Timestamp.from(startsAt));
                if (conflicts != null && conflicts > 0) throw new BusinessException("SCHEDULE_CONFLICT", "O profissional já possui horário neste período.", HttpStatus.CONFLICT);
                jdbc.update("INSERT INTO time_slots(schedule_id,starts_at,ends_at,capacity) VALUES (?,?,?,?)",
                        scheduleId, Timestamp.from(startsAt), Timestamp.from(endsAt), request.capacity());
                slotsCreated++;
            }
        }
        return slotsCreated;
    }

    public record CreateSchedule(@NotNull UUID professionalId, @NotNull UUID unitId, UUID roomId,
                                 @NotNull LocalDate validFrom, @NotNull LocalDate validUntil,
                                 @NotEmpty Set<Integer> daysOfWeek, @NotNull LocalTime startTime, @NotNull LocalTime endTime,
                                 LocalTime breakStart, LocalTime breakEnd,
                                 @Min(5) @Max(480) int slotDurationMinutes, @Min(1) @Max(100) int capacity) {}
    public record ActiveRequest(boolean active) {}
    public record CapacityRequest(@Min(1) @Max(100) int capacity) {}
    public record UpdateSchedule(@NotNull UUID professionalId, @NotNull UUID unitId, UUID roomId,
                                 @NotNull LocalDate validFrom, @NotNull LocalDate validUntil,
                                 @NotEmpty Set<Integer> daysOfWeek, @NotNull LocalTime startTime, @NotNull LocalTime endTime,
                                 LocalTime breakStart, LocalTime breakEnd,
                                 @Min(5) @Max(480) int slotDurationMinutes, @Min(1) @Max(100) int capacity) {}
    private record ScheduleIdentity(UUID professionalId, UUID unitId) {}
}
