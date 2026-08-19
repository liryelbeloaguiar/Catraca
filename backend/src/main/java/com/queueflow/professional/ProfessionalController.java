package com.queueflow.professional;

import java.util.List;
import java.util.Map;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/professionals")
public class ProfessionalController {
    private final JdbcTemplate jdbc;
    public ProfessionalController(JdbcTemplate jdbc) { this.jdbc = jdbc; }

    @GetMapping
    List<Map<String,Object>> list(@RequestParam(defaultValue="0") int page, @RequestParam(defaultValue="100") int size) {
        int safeSize = Math.min(Math.max(size, 1), 100);
        return jdbc.queryForList("""
                SELECT p.id,u.full_name,u.email,p.registration_number,p.default_duration_minutes,
                       pt.name professional_type_name,p.active
                FROM professionals p JOIN users u ON u.id=p.user_id
                JOIN professional_types pt ON pt.id=p.professional_type_id
                ORDER BY u.full_name LIMIT ? OFFSET ?
                """, safeSize, Math.max(page, 0) * safeSize);
    }
}
