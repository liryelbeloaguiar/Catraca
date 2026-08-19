package com.queueflow.schedule;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;

@RestController @RequestMapping("/api/v1/time-slots")
public class TimeSlotController {
    private final JdbcTemplate jdbc;
    public TimeSlotController(JdbcTemplate jdbc) { this.jdbc = jdbc; }
    @GetMapping
    List<TimeSlotResponse> available(@RequestParam UUID unitId, @RequestParam Instant from, @RequestParam Instant to, @RequestParam(required=false) UUID professionalId) {
        return jdbc.query("SELECT ts.id,ts.starts_at,ts.ends_at,ts.capacity,ts.booked_count,ts.blocked,sc.professional_id,u.full_name professional_name FROM time_slots ts JOIN schedules sc ON sc.id=ts.schedule_id JOIN professionals p ON p.id=sc.professional_id JOIN users u ON u.id=p.user_id WHERE sc.unit_id=? AND sc.active=true AND ts.starts_at>=? AND ts.starts_at<? AND (?::uuid IS NULL OR sc.professional_id=?) ORDER BY ts.starts_at",
                (rs,row) -> new TimeSlotResponse(UUID.fromString(rs.getString("id")), rs.getTimestamp("starts_at").toInstant(), rs.getTimestamp("ends_at").toInstant(), rs.getInt("capacity"), rs.getInt("booked_count"), Math.max(0, rs.getInt("capacity")-rs.getInt("booked_count")), rs.getBoolean("blocked"), UUID.fromString(rs.getString("professional_id")), rs.getString("professional_name")), unitId, java.sql.Timestamp.from(from), java.sql.Timestamp.from(to), professionalId, professionalId);
    }
    record TimeSlotResponse(UUID id, Instant startsAt, Instant endsAt, int capacity, int bookedCount,
                            int availableCapacity, boolean blocked, UUID professionalId, String professionalName) {}
}
