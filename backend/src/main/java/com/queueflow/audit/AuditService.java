package com.queueflow.audit;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

@Service
public class AuditService {
    private final JdbcTemplate jdbc;
    private final ObjectMapper mapper;
    private final ObjectProvider<HttpServletRequest> requestProvider;
    public AuditService(JdbcTemplate jdbc, ObjectMapper mapper, ObjectProvider<HttpServletRequest> requestProvider) {
        this.jdbc = jdbc; this.mapper = mapper; this.requestProvider = requestProvider;
    }
    public void record(UUID actorId, String action, String resourceType, UUID resourceId, Map<String, ?> details) {
        try {
            var request = requestProvider.getIfAvailable();
            var enriched = new LinkedHashMap<String, Object>();
            enriched.putAll(details);
            String ipAddress = null;
            if (request != null) {
                ipAddress = clientIp(request);
                enriched.put("httpMethod", request.getMethod());
                enriched.put("requestPath", request.getRequestURI());
                enriched.put("userAgent", truncate(request.getHeader("User-Agent"), 300));
            }
            jdbc.update("INSERT INTO audit_logs(actor_user_id,action,resource_type,resource_id,details,ip_address) VALUES (?,?,?,?,?::jsonb,?::inet)",
                    actorId, action, resourceType, resourceId, mapper.writeValueAsString(enriched), ipAddress);
        } catch (JsonProcessingException exception) { throw new IllegalStateException("Could not serialize audit details", exception); }
    }

    private String clientIp(HttpServletRequest request) {
        var forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) return forwarded.split(",")[0].trim();
        var realIp = request.getHeader("X-Real-IP");
        return realIp == null || realIp.isBlank() ? request.getRemoteAddr() : realIp;
    }

    private String truncate(String value, int limit) {
        if (value == null) return null;
        return value.length() <= limit ? value : value.substring(0, limit);
    }
}
