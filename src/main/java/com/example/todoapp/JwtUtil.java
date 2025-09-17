package com.example.todoapp;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;

@Component
public class JwtUtil {

    private final SecretKey key;
    private final long jwtExpirationMs;

    public JwtUtil(
            @Value("${jwt.secret:}") String secret,
            @Value("${jwt.expirationMs:86400000}") long jwtExpirationMs
    ) {
        if (secret == null || secret.length() < 32) {
            this.key = Keys.secretKeyFor(SignatureAlgorithm.HS256);
            System.out.println("⚠️ Using generated JWT key (tests/dev only)");
        } else {
            this.key = Keys.hmacShaKeyFor(secret.getBytes());
        }
        this.jwtExpirationMs = jwtExpirationMs;
    }

    public String generateToken(String username, Set<String> roles) {
        return Jwts.builder()
                .setSubject(username)
                .claim("roles", roles) // always a Set<String>
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + jwtExpirationMs))
                .signWith(key, SignatureAlgorithm.HS256)
                .compact();
    }

    public String validateTokenAndGetUsername(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(key)
                .build()
                .parseClaimsJws(token)
                .getBody()
                .getSubject();
    }

    @SuppressWarnings("unchecked")
    public Set<String> getRoles(String token) {
        Object claim = Jwts.parserBuilder()
                .setSigningKey(key)
                .build()
                .parseClaimsJws(token)
                .getBody()
                .get("roles");

        if (claim instanceof String role) {
            return Set.of(role);
        } else if (claim instanceof Iterable<?> iterable) {
            Set<String> roles = new HashSet<>();
            for (Object r : iterable) {
                if (r != null) roles.add(r.toString());
            }
            return roles;
        }
        return Collections.emptySet();
    }

    public boolean validateToken(String token) {
        try {
            Jwts.parserBuilder()
                    .setSigningKey(key)
                    .build()
                    .parseClaimsJws(token);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public String extractUsername(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(key)
                .build()
                .parseClaimsJws(token)
                .getBody()
                .getSubject();
    }
}
