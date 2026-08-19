package com.queueflow.auth;

import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.stereotype.Service;

@Service
public class TokenService {
    private final JwtEncoder encoder;
    private final long accessMinutes;

    public TokenService(JwtEncoder encoder, @Value("${app.security.access-token-minutes}") long accessMinutes) {
        this.encoder = encoder;
        this.accessMinutes = accessMinutes;
    }

    String accessToken(UUID userId, String email, List<String> authorities) {
        var now = Instant.now();
        var claims = JwtClaimsSet.builder().issuer("catraca").issuedAt(now)
                .expiresAt(now.plus(accessMinutes, ChronoUnit.MINUTES)).subject(userId.toString())
                .claim("email", email).claim("authorities", authorities).build();
        var header = JwsHeader.with(MacAlgorithm.HS256).build();
        return encoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
    }

    Duration accessDuration() { return Duration.ofMinutes(accessMinutes); }
}
