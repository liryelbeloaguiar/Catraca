package com.queueflow.auth;

import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class AdminBootstrap implements ApplicationRunner {
    private final JdbcTemplate jdbc;
    private final PasswordEncoder encoder;
    private final String email;
    private final String password;

    public AdminBootstrap(JdbcTemplate jdbc, PasswordEncoder encoder,
                          @Value("${app.bootstrap.admin-email:}") String email,
                          @Value("${app.bootstrap.admin-password:}") String password) {
        this.jdbc = jdbc; this.encoder = encoder; this.email = email; this.password = password;
    }

    @Override @Transactional
    public void run(ApplicationArguments args) {
        if (email.isBlank() || password.isBlank()) return;
        Integer count = jdbc.queryForObject("SELECT count(*) FROM users WHERE lower(email)=lower(?)", Integer.class, email);
        if (count != null && count > 0) return;
        UUID id = UUID.randomUUID();
        jdbc.update("INSERT INTO users(id,email,password_hash,full_name) VALUES (?,?,?,?)", id, email.toLowerCase(), encoder.encode(password), "Administrador");
        jdbc.update("INSERT INTO user_roles(user_id,role_id) SELECT ?,id FROM roles WHERE code='ADMIN'", id);
    }
}
