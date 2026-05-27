package com.ainions.nion.security;

import com.ainions.nion.config.NionProperties;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.time.Instant;
import java.util.Date;
import org.springframework.stereotype.Service;

@Service
public class JwtService {

    private final NionProperties properties;

    public JwtService(NionProperties properties) {
        this.properties = properties;
    }

    public String generateToken(UserPrincipal principal) {
        Instant now = Instant.now();
        Instant expiry = now.plusSeconds(properties.jwt().accessTokenMinutes() * 60L);
        return Jwts.builder()
                .setSubject(principal.username())
                .setIssuer(properties.jwt().issuer())
                .setIssuedAt(Date.from(now))
                .setExpiration(Date.from(expiry))
                .claim("tenantId", principal.tenantId())
                .claim("roles", principal.authorities().stream().map(Object::toString).toList())
                .signWith(signingKey(), SignatureAlgorithm.HS256)
                .compact();
    }

    public boolean isTokenValid(String token, UserPrincipal principal) {
        Claims claims = parseClaims(token);
        return claims.getSubject().equals(principal.username())
                && claims.getExpiration().after(new Date());
    }

    public Claims parseClaims(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(signingKey())
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    private Key signingKey() {
        String secret = properties.jwt().secret();
        if (secret == null || secret.isBlank()) {
            secret = "dev-secret-change-me-dev-secret-change-me";
        }
        if (secret.startsWith("base64:")) {
            byte[] decoded = Decoders.BASE64.decode(secret.substring("base64:".length()));
            return Keys.hmacShaKeyFor(decoded);
        }
        return Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }
}
