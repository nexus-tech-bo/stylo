package com.example.styloflow.security;

import com.example.styloflow.enums.Role;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.Optional;

/**
 * Issues and parses the HS256 tokens. Business token claims: {@code sub}=userId, {@code nid}=businessId,
 * {@code role}, {@code name}. Platform token: {@code role}=SUPERADMIN and no {@code nid}.
 */
@Component
public class JwtService {
    static final String BUSINESS_ID = "nid";
    static final String ROLE = "role";
    static final String NAME = "name";

    private final SecretKey key;
    private final Duration ttl;

    public JwtService(@Value("${jwt.secret}") String base64Secret, @Value("${jwt.expiration-hours:12}") long expirationHours) {
        this.key = Keys.hmacShaKeyFor(Decoders.BASE64.decode(base64Secret));
        this.ttl = Duration.ofHours(expirationHours);
    }

    public record IssuedToken(String token, Instant expiresAt) {
    }

    public IssuedToken issueBusinessToken(Long userId, Long businessId, Role role, String name) {
        return issue(userId, businessId, role, name);
    }

    public IssuedToken issuePlatformToken(Long adminId, String name) {
        return issue(adminId, null, Role.SUPERADMIN, name);
    }

    /**
     * Verifies signature and expiration.
     *
     * @param token raw JWT
     * @return the principal, or empty when the token is invalid, expired or malformed
     */
    public Optional<AuthenticatedUser> parse(String token) {
        try {
            Claims claims = Jwts.parser().verifyWith(key).build().parseSignedClaims(token).getPayload();
            var nid = claims.get(BUSINESS_ID, Number.class);
            return Optional.of(new AuthenticatedUser(
                    Long.valueOf(claims.getSubject()),
                    nid == null ? null : nid.longValue(),
                    Role.valueOf(claims.get(ROLE, String.class)),
                    claims.get(NAME, String.class)));
        } catch (JwtException | IllegalArgumentException | NullPointerException _) {
            return Optional.empty();
        }
    }

    private IssuedToken issue(Long subject, Long businessId, Role role, String name) {
        var now = Instant.now().truncatedTo(ChronoUnit.SECONDS);
        var expiresAt = now.plus(ttl);
        var builder = Jwts.builder()
                .subject(subject.toString())
                .claim(ROLE, role.name())
                .claim(NAME, name)
                .issuedAt(Date.from(now))
                .expiration(Date.from(expiresAt));
        if (businessId != null) builder.claim(BUSINESS_ID, businessId);
        return new IssuedToken(builder.signWith(key).compact(), expiresAt);
    }
}
