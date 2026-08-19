package com.queueflow.queue;

import com.queueflow.audit.AuditService;
import com.queueflow.common.BusinessException;
import java.time.LocalDate;
import java.util.Map;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@Service
public class QueueService {
    private final JdbcTemplate jdbc; private final AuditService audit; private final QueueOrderingService ordering;
    private final DisplayPanelStreamService stream;
    public QueueService(JdbcTemplate jdbc, AuditService audit, QueueOrderingService ordering,
                        DisplayPanelStreamService stream) {
        this.jdbc=jdbc; this.audit=audit; this.ordering=ordering; this.stream=stream;
    }
    @Transactional
    public Ticket enqueue(UUID queueId, UUID patientId, UUID appointmentId, UUID priorityId, UUID actorId) {
        var prefixes = jdbc.queryForList("SELECT ticket_prefix FROM queues WHERE id=? AND active=true", String.class, queueId);
        if (prefixes.isEmpty()) throw new BusinessException("QUEUE_NOT_FOUND", "Fila não encontrada.", HttpStatus.NOT_FOUND);
        Long number = jdbc.queryForObject("INSERT INTO ticket_sequences(queue_id,sequence_date,last_value) VALUES (?,current_date,1) ON CONFLICT(queue_id,sequence_date) DO UPDATE SET last_value=ticket_sequences.last_value+1 RETURNING last_value", Long.class, queueId);
        String display = prefixes.getFirst()+"-"+String.format("%03d", number); UUID ticketId=UUID.randomUUID(); UUID entryId=UUID.randomUUID();
        jdbc.update("INSERT INTO tickets(id,queue_id,sequence_date,sequence_number,display_code) VALUES (?,?,current_date,?,?)", ticketId,queueId,number,display);
        jdbc.update("INSERT INTO queue_entries(id,queue_id,ticket_id,appointment_id,patient_id,priority_id,scheduled_at,status) VALUES (?,?,?,?,?,?,(SELECT ts.starts_at FROM appointments a JOIN time_slots ts ON ts.id=a.time_slot_id WHERE a.id=?),'WAITING')", entryId,queueId,ticketId,appointmentId,patientId,priorityId,appointmentId);
        audit.record(actorId,"TICKET_CREATE","ticket",ticketId,Map.of("displayCode",display)); return new Ticket(ticketId,display);
    }
    @Transactional
    public Map<String,Object> callNext(UUID queueId, UUID actorId, UUID counterId, UUID roomId) {
        validateDestination(queueId, counterId, roomId);
        var rows=jdbc.queryForList("SELECT qe.id,qe.patient_id,t.id ticket_id,t.display_code FROM queue_entries qe JOIN tickets t ON t.id=qe.ticket_id LEFT JOIN priorities p ON p.id=qe.priority_id WHERE qe.queue_id=? AND qe.status='WAITING' ORDER BY " + ordering.databaseOrderBy() + " FOR UPDATE OF qe SKIP LOCKED LIMIT 1",queueId);
        if(rows.isEmpty()) throw new BusinessException("QUEUE_EMPTY","Não há pessoas aguardando nesta fila.",HttpStatus.CONFLICT);
        var row = rows.getFirst();
        UUID entryId=(UUID)row.get("id");
        UUID ticketId=(UUID)row.get("ticket_id");
        UUID patientId=(UUID)row.get("patient_id");
        jdbc.update("UPDATE queue_entries SET status='CALLED',called_at=now() WHERE id=?",entryId);
        int attendanceChanged = jdbc.update("UPDATE attendances SET attendant_user_id=?,room_id=?,counter_id=?,status='CALLED' WHERE ticket_id=?",
                actorId, roomId, counterId, ticketId);
        if (attendanceChanged == 0) {
            jdbc.update("INSERT INTO attendances(id,ticket_id,patient_id,attendant_user_id,room_id,counter_id,status) VALUES (gen_random_uuid(),?,?,?,?,?,'CALLED')",
                    ticketId, patientId, actorId, roomId, counterId);
        }
        jdbc.update("""
                INSERT INTO queue_call_events(
                    id,panel_id,queue_entry_id,ticket_id,room_id,counter_id,call_number,called_at
                )
                SELECT gen_random_uuid(),relation.panel_id,?,?,?, ?,1,now()
                FROM display_panel_queues relation
                JOIN display_panels panel ON panel.id=relation.panel_id AND panel.active=true
                WHERE relation.queue_id=?
                ON CONFLICT(panel_id,queue_entry_id,call_number) DO UPDATE
                SET room_id=EXCLUDED.room_id,counter_id=EXCLUDED.counter_id,called_at=now()
                """, entryId, ticketId, roomId, counterId, queueId);
        var panelTokens = jdbc.queryForList("""
                SELECT panel.public_token
                FROM display_panels panel
                JOIN display_panel_queues relation ON relation.panel_id=panel.id
                WHERE relation.queue_id=? AND panel.active=true
                """, UUID.class, queueId);
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override public void afterCommit() {
                panelTokens.forEach(stream::refresh);
            }
        });
        audit.record(actorId,"QUEUE_CALL","queue_entry",entryId,Map.of("ticket",row.get("display_code")));
        return Map.of("id", entryId, "displayCode", row.get("display_code"),
                "counterId", counterId == null ? "" : counterId,
                "roomId", roomId == null ? "" : roomId);
    }
    private void validateDestination(UUID queueId, UUID counterId, UUID roomId) {
        if (counterId != null) {
            Integer valid = jdbc.queryForObject("SELECT count(*) FROM counters c JOIN queues q ON q.unit_id=c.unit_id WHERE c.id=? AND q.id=? AND c.active=true", Integer.class, counterId, queueId);
            if (valid == null || valid == 0) throw new BusinessException("INVALID_COUNTER", "Guichê inválido para esta fila.", HttpStatus.BAD_REQUEST);
        }
        if (roomId != null) {
            Integer valid = jdbc.queryForObject("SELECT count(*) FROM rooms r JOIN queues q ON q.unit_id=r.unit_id WHERE r.id=? AND q.id=? AND r.active=true", Integer.class, roomId, queueId);
            if (valid == null || valid == 0) throw new BusinessException("INVALID_ROOM", "Sala inválida para esta fila.", HttpStatus.BAD_REQUEST);
        }
    }
    public record Ticket(UUID id,String displayCode){}
}
