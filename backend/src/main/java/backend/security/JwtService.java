package backend.security;

import java.nio.charset.StandardCharsets;
import java.util.Date;

import javax.crypto.SecretKey;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

@Service
public class JwtService {

    private final SecretKey key;
    private final long expirationMs;

    public JwtService(
            @Value("${app.jwt.secret}") String secret,
            @Value("${app.jwt.expiration-minutes}") long expirationMinutes
    ) {
        // secret doit être suffisamment long (>= 32 chars) pour HS256
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.expirationMs = expirationMinutes * 60_000L;
    }

    public String generateToken(backend.entity.User user) {
        Date now = new Date();
        Date exp = new Date(now.getTime() + expirationMs);

        return Jwts.builder()
                .subject(String.valueOf(user.getId()))
                .claim("sv", user.getSessionVersion())
                .issuedAt(now)
                .expiration(exp)
                .signWith(key)
                .compact();
    }

    public Long extractUserId(String token) {
        String sub = Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload()
                .getSubject();

        return Long.valueOf(sub);
    }

    public boolean matchesSession(String token, backend.entity.User user) {
        var claims = Jwts.parser().verifyWith(key).build().parseSignedClaims(token).getPayload();
        Number version = claims.get("sv", Number.class);
        return version != null && version.longValue() == user.getSessionVersion() && user.isEmailVerified();
    }

    public boolean isValid(String token) {
        try {
            Jwts.parser().verifyWith(key).build().parseSignedClaims(token);
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}
