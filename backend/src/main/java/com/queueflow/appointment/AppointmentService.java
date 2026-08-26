package com.queueflow.appointment;

import com.queueflow.audit.AuditService;
import com.queueflow.common.BusinessException;
import java.util.Map;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AppointmentService {
    private final JdbcTemplate jdbc;
    private final AuditService audit;
    public AppointmentService(JdbcTemplate jdbc, AuditService audit) { this.jdbc = jdbc; this.audit = audit; }

    @Transactional(isolation = Isolation.READ_COMMITTED)
    public UUID create(CreateAppointment command, UUID actorId, boolean canOverbook) {
        if (command.patientId() == null && (command.guestName() == null || command.guestName().isBlank())) {
            throw new BusinessException("APPOINTMENT_PATIENT_REQUIRED", "Informe o paciente ou o nome para atendimento.", HttpStatus.BAD_REQUEST);
        }
        var slots = jdbc.query("""
                SELECT ts.capacity,ts.booked_count,ts.blocked,sc.unit_id,sc.professional_id
                FROM time_slots ts JOIN schedules sc ON sc.id=ts.schedule_id
                WHERE ts.id=? FOR UPDATE OF ts
                """, (result, row) -> new Slot(result.getInt("capacity"), result.getInt("booked_count"),
                result.getBoolean("blocked"), UUID.fromString(result.getString("unit_id")),
                UUID.fromString(result.getString("professional_id"))), command.timeSlotId());
        if (slots.isEmpty()) throw new BusinessException("TIME_SLOT_NOT_FOUND", "Horário não encontrado.", HttpStatus.NOT_FOUND);
        var slot = slots.getFirst();
        if (!slot.unitId().equals(command.unitId())) throw new BusinessException("INVALID_APPOINTMENT_UNIT", "O horário não pertence à unidade selecionada.", HttpStatus.BAD_REQUEST);
        if (slot.blocked()) throw new BusinessException("TIME_SLOT_BLOCKED", "Este horário está indisponível.", HttpStatus.CONFLICT);

        var services = jdbc.query("SELECT requires_professional,requires_counter,active FROM services WHERE id=?",
                (result, row) -> new ServiceRules(result.getBoolean("requires_professional"), result.getBoolean("requires_counter"), result.getBoolean("active")), command.serviceId());
        if (services.isEmpty() || !services.getFirst().active()) throw new BusinessException("SERVICE_NOT_AVAILABLE", "Serviço indisponível.", HttpStatus.BAD_REQUEST);
        var rules = services.getFirst();
        UUID professionalId = command.professionalId() == null ? slot.professionalId() : command.professionalId();
        if (rules.requiresProfessional() && professionalId == null) throw new BusinessException("PROFESSIONAL_REQUIRED", "Este serviço exige um profissional.", HttpStatus.BAD_REQUEST);
        if (rules.requiresCounter() && command.counterId() == null) throw new BusinessException("COUNTER_REQUIRED", "Selecione o guichê para este serviço.", HttpStatus.BAD_REQUEST);
        if (command.counterId() != null) {
            Integer valid = jdbc.queryForObject("SELECT count(*) FROM counters WHERE id=? AND unit_id=? AND active=true", Integer.class, command.counterId(), command.unitId());
            if (valid == null || valid == 0) throw new BusinessException("INVALID_COUNTER", "Guichê inválido para a unidade.", HttpStatus.BAD_REQUEST);
        }

        boolean overbook = slot.booked() >= slot.capacity();
        if (overbook && (!command.allowOverbook() || !canOverbook)) {
            throw new BusinessException("APPOINTMENT_SLOT_FULL", "Não existem mais vagas disponíveis para este horário.", HttpStatus.CONFLICT);
        }
        UUID id = UUID.randomUUID();
        jdbc.update("""
                INSERT INTO appointments(id,patient_id,guest_name,professional_id,specialty_id,service_id,
                    unit_id,time_slot_id,counter_id,status,overbook,created_by)
                VALUES (?,?,?,?,?,?,?,?,?,?,?,?)
                """, id, command.patientId(), clean(command.guestName()), professionalId, command.specialtyId(),
                command.serviceId(), command.unitId(), command.timeSlotId(), command.counterId(),
                AppointmentStatus.SCHEDULED.name(), overbook, actorId);
        if (!overbook) jdbc.update("UPDATE time_slots SET booked_count=booked_count+1,version=version+1 WHERE id=?", command.timeSlotId());
        jdbc.update("INSERT INTO appointment_history(appointment_id,new_status,changed_by) VALUES (?,?,?)", id, AppointmentStatus.SCHEDULED.name(), actorId);
        audit.record(actorId, overbook ? "APPOINTMENT_OVERBOOK" : "APPOINTMENT_CREATE", "appointment", id,
                Map.of("timeSlotId", command.timeSlotId(), "guest", command.patientId() == null));
        return id;
    }

    @Transactional
    public void changeStatus(UUID id, AppointmentStatus target, UUID actorId, boolean patientOnly) {
        var rows = jdbc.queryForList("""
                SELECT appointment.status,appointment.time_slot_id,patient.user_id patient_user_id
                FROM appointments appointment
                LEFT JOIN patients patient ON patient.id=appointment.patient_id
                WHERE appointment.id=? FOR UPDATE OF appointment
                """, id);
        if (rows.isEmpty()) throw new BusinessException("APPOINTMENT_NOT_FOUND", "Agendamento não encontrado.", HttpStatus.NOT_FOUND);
        if (patientOnly) {
            Object owner = rows.getFirst().get("patient_user_id");
            if (!actorId.equals(owner) || target != AppointmentStatus.CANCELLED) {
                throw new org.springframework.security.access.AccessDeniedException("Patient cannot change this appointment");
            }
        }
        var current = AppointmentStatus.valueOf((String) rows.getFirst().get("status"));
        if (!allowed(current, target)) throw new BusinessException("INVALID_APPOINTMENT_STATUS", "Transição de status não permitida.", HttpStatus.CONFLICT);
        jdbc.update("UPDATE appointments SET status=?,checked_in_at=CASE WHEN ?='CHECKED_IN' THEN now() ELSE checked_in_at END,updated_at=now() WHERE id=?", target.name(), target.name(), id);
        jdbc.update("INSERT INTO appointment_history(appointment_id,previous_status,new_status,changed_by) VALUES (?,?,?,?)", id, current.name(), target.name(), actorId);
        if (target == AppointmentStatus.CANCELLED && current != AppointmentStatus.CANCELLED) {
            jdbc.update("UPDATE time_slots SET booked_count=greatest(booked_count-1,0),version=version+1 WHERE id=? AND EXISTS(SELECT 1 FROM appointments WHERE id=? AND overbook=false)", rows.getFirst().get("time_slot_id"), id);
        }
        audit.record(actorId, "APPOINTMENT_STATUS_CHANGE", "appointment", id, Map.of("from", current.name(), "to", target.name()));
    }

    private boolean allowed(AppointmentStatus from, AppointmentStatus to) {
        return switch (from) {
            case SCHEDULED -> to == AppointmentStatus.CONFIRMED || to == AppointmentStatus.CANCELLED || to == AppointmentStatus.RESCHEDULED;
            case CONFIRMED -> to == AppointmentStatus.CHECKED_IN || to == AppointmentStatus.CANCELLED || to == AppointmentStatus.NO_SHOW || to == AppointmentStatus.RESCHEDULED;
            case CHECKED_IN -> to == AppointmentStatus.WAITING || to == AppointmentStatus.CANCELLED;
            case WAITING -> to == AppointmentStatus.CALLED || to == AppointmentStatus.CANCELLED;
            case CALLED -> to == AppointmentStatus.IN_SERVICE || to == AppointmentStatus.NO_SHOW || to == AppointmentStatus.WAITING;
            case IN_SERVICE -> to == AppointmentStatus.COMPLETED;
            default -> false;
        };
    }

    private String clean(String value) { return value == null ? null : value.trim(); }
    private record Slot(int capacity, int booked, boolean blocked, UUID unitId, UUID professionalId) {}
    private record ServiceRules(boolean requiresProfessional, boolean requiresCounter, boolean active) {}
    public record CreateAppointment(UUID patientId, String guestName, UUID professionalId, UUID specialtyId,
                                    UUID serviceId, UUID unitId, UUID timeSlotId, UUID counterId, boolean allowOverbook) {}
}
