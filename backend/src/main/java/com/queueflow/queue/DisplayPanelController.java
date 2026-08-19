package com.queueflow.queue;

import com.queueflow.audit.AuditService;
import com.queueflow.common.BusinessException;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@RestController
@RequestMapping("/api/v1")
public class DisplayPanelController {
    private final JdbcTemplate jdbc;
    private final AuditService audit;
    private final DisplayPanelStreamService stream;

    public DisplayPanelController(JdbcTemplate jdbc, AuditService audit, DisplayPanelStreamService stream) {
        this.jdbc = jdbc;
        this.audit = audit;
        this.stream = stream;
    }

    @GetMapping("/display-panels")
    @PreAuthorize("hasAnyAuthority('QUEUE_READ', 'ADMINISTRATION_MANAGE')")
    List<PanelSummary> list() {
        return jdbc.query("""
                SELECT panel.id, panel.code, panel.name, panel.floor, panel.public_token,
                       panel.audio_enabled, panel.voice_enabled, panel.last_calls_limit,
                       panel.active, panel.unit_id, unit.name unit_name,
                       COALESCE(string_agg(queue.name, ', ' ORDER BY queue.name), '') queue_names
                FROM display_panels panel
                JOIN units unit ON unit.id=panel.unit_id
                LEFT JOIN display_panel_queues relation ON relation.panel_id=panel.id
                LEFT JOIN queues queue ON queue.id=relation.queue_id
                GROUP BY panel.id, unit.name
                ORDER BY unit.name, panel.floor, panel.name
                """, (result, row) -> new PanelSummary(
                UUID.fromString(result.getString("id")),
                result.getString("code"),
                result.getString("name"),
                result.getString("floor"),
                UUID.fromString(result.getString("public_token")),
                result.getBoolean("audio_enabled"),
                result.getBoolean("voice_enabled"),
                result.getInt("last_calls_limit"),
                result.getBoolean("active"),
                UUID.fromString(result.getString("unit_id")),
                result.getString("unit_name"),
                result.getString("queue_names")));
    }

    @PostMapping("/display-panels")
    @PreAuthorize("hasAuthority('ADMINISTRATION_MANAGE')")
    @Transactional
    PanelSummary create(
            @Valid @RequestBody CreatePanel request,
            @AuthenticationPrincipal Jwt jwt) {
        validateQueues(request.unitId(), request.queueIds());
        UUID panelId = UUID.randomUUID();
        UUID publicToken = UUID.randomUUID();
        jdbc.update("""
                INSERT INTO display_panels(
                    id, unit_id, code, name, floor, public_token,
                    audio_enabled, voice_enabled, last_calls_limit, active
                ) VALUES (?,?,?,?,?,?,?,?,?,true)
                """, panelId, request.unitId(), request.code().trim().toUpperCase(),
                request.name().trim(), clean(request.floor()), publicToken,
                request.audioEnabled(), request.voiceEnabled(), request.lastCallsLimit());
        request.queueIds().forEach(queueId -> jdbc.update(
                "INSERT INTO display_panel_queues(panel_id,queue_id) VALUES (?,?)",
                panelId, queueId));
        audit.record(UUID.fromString(jwt.getSubject()), "DISPLAY_PANEL_CREATE",
                "display_panel", panelId, Map.of("queues", request.queueIds().size()));
        return find(panelId);
    }

    @PatchMapping("/display-panels/{panelId}/active")
    @PreAuthorize("hasAuthority('ADMINISTRATION_MANAGE')")
    void active(
            @PathVariable UUID panelId,
            @RequestBody ActiveRequest request,
            @AuthenticationPrincipal Jwt jwt) {
        int changed = jdbc.update(
                "UPDATE display_panels SET active=?,updated_at=now() WHERE id=?",
                request.active(), panelId);
        if (changed == 0) {
            throw new BusinessException("DISPLAY_PANEL_NOT_FOUND",
                    "Painel não encontrado.", HttpStatus.NOT_FOUND);
        }
        audit.record(UUID.fromString(jwt.getSubject()), "DISPLAY_PANEL_ACTIVE_CHANGE",
                "display_panel", panelId, Map.of("active", request.active()));
    }

    @GetMapping("/public/display-panels/{publicToken}")
    PublicPanel publicPanel(@PathVariable UUID publicToken) {
        var panels = jdbc.query("""
                SELECT panel.id, panel.name, panel.floor, panel.audio_enabled,
                       panel.voice_enabled, panel.last_calls_limit, unit.name unit_name
                FROM display_panels panel
                JOIN units unit ON unit.id=panel.unit_id
                WHERE panel.public_token=? AND panel.active=true
                """, (result, row) -> new PublicPanelHeader(
                UUID.fromString(result.getString("id")),
                result.getString("name"), result.getString("floor"),
                result.getString("unit_name"), result.getBoolean("audio_enabled"),
                result.getBoolean("voice_enabled"), result.getInt("last_calls_limit")),
                publicToken);
        if (panels.isEmpty()) {
            throw new BusinessException("DISPLAY_PANEL_NOT_FOUND",
                    "Painel indisponível.", HttpStatus.NOT_FOUND);
        }
        var panel = panels.getFirst();
        var calls = jdbc.query("""
                SELECT ticket.display_code, entry.status, event.called_at,
                       queue.name queue_name, room.name room_name, room.floor,
                       counter.name counter_name
                FROM queue_call_events event
                JOIN queue_entries entry ON entry.id=event.queue_entry_id
                JOIN tickets ticket ON ticket.id=event.ticket_id
                JOIN queues queue ON queue.id=entry.queue_id
                LEFT JOIN rooms room ON room.id=event.room_id
                LEFT JOIN counters counter ON counter.id=event.counter_id
                WHERE event.panel_id=? AND ticket.sequence_date=current_date
                ORDER BY event.called_at DESC
                LIMIT ?
                """, (result, row) -> new PublicCall(
                result.getString("display_code"), result.getString("status"),
                result.getTimestamp("called_at").toInstant(),
                result.getString("queue_name"), result.getString("room_name"),
                result.getString("floor"), result.getString("counter_name")),
                panel.id(), panel.lastCallsLimit());
        return new PublicPanel(panel.name(), panel.unitName(), panel.floor(),
                panel.audioEnabled(), panel.voiceEnabled(), Instant.now(), calls);
    }

    @GetMapping(value = "/public/display-panels/{publicToken}/events",
            produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    SseEmitter events(@PathVariable UUID publicToken) {
        Integer available = jdbc.queryForObject(
                "SELECT count(*) FROM display_panels WHERE public_token=? AND active=true",
                Integer.class, publicToken);
        if (available == null || available == 0) {
            throw new BusinessException("DISPLAY_PANEL_NOT_FOUND",
                    "Painel indisponível.", HttpStatus.NOT_FOUND);
        }
        return stream.connect(publicToken);
    }

    private PanelSummary find(UUID panelId) {
        return list().stream().filter(panel -> panel.id().equals(panelId)).findFirst()
                .orElseThrow(() -> new BusinessException("DISPLAY_PANEL_NOT_FOUND",
                        "Painel não encontrado.", HttpStatus.NOT_FOUND));
    }

    private void validateQueues(UUID unitId, List<UUID> queueIds) {
        for (UUID queueId : queueIds) {
            Integer valid = jdbc.queryForObject("""
                    SELECT count(*) FROM queues
                    WHERE unit_id=? AND active=true AND id=?
                    """, Integer.class, unitId, queueId);
            if (valid == null || valid == 0) {
                throw new BusinessException("INVALID_PANEL_QUEUES",
                        "Selecione apenas filas ativas da mesma unidade.",
                        HttpStatus.BAD_REQUEST);
            }
        }
    }

    private String clean(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    public record CreatePanel(
            @NotNull UUID unitId,
            @NotBlank @Size(max = 40) String code,
            @NotBlank @Size(max = 140) String name,
            @Size(max = 30) String floor,
            @NotEmpty List<UUID> queueIds,
            boolean audioEnabled,
            boolean voiceEnabled,
            @Min(1) @Max(20) int lastCallsLimit) {}

    public record ActiveRequest(boolean active) {}

    public record PanelSummary(
            UUID id, String code, String name, String floor, UUID publicToken,
            boolean audioEnabled, boolean voiceEnabled, int lastCallsLimit,
            boolean active, UUID unitId, String unitName, String queueNames) {}

    private record PublicPanelHeader(
            UUID id, String name, String floor, String unitName,
            boolean audioEnabled, boolean voiceEnabled, int lastCallsLimit) {}

    public record PublicPanel(
            String name, String unitName, String floor, boolean audioEnabled,
            boolean voiceEnabled, Instant serverTime, List<PublicCall> calls) {}

    public record PublicCall(
            String displayCode, String status, Instant calledAt, String queueName,
            String roomName, String floor, String counterName) {}
}
