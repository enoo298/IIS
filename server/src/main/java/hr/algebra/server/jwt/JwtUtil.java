package hr.algebra.server.jwt;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Component;

import java.util.Date;

@Component
public class JwtUtil {
    private final String ACCESS_SECRET = "12345678901234567890_access_secret";
    private final String REFRESH_SECRET = "12345678901234567890_refresh_secret";
    private final long ACCESS_EXPIRATION = 1000 * 30; // 30 sekundi
    private final long REFRESH_EXPIRATION = 1000 * 60 * 10; // 10 minuta

    public String generateAccessToken(String username) {
        return Jwts.builder()
                .setSubject(username)
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + ACCESS_EXPIRATION))
                .signWith(Keys.hmacShaKeyFor(ACCESS_SECRET.getBytes()), SignatureAlgorithm.HS256)
                .compact();
    }

    public String generateRefreshToken(String username) {
        return Jwts.builder()
                .setSubject(username)
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + REFRESH_EXPIRATION))
                .signWith(Keys.hmacShaKeyFor(REFRESH_SECRET.getBytes()), SignatureAlgorithm.HS256)
                .compact();
    }

    public String extractUsernameFromAccessToken(String token) {
        return Jwts.parser()
                .setSigningKey(Keys.hmacShaKeyFor(ACCESS_SECRET.getBytes()))
                .parseClaimsJws(token)
                .getBody()
                .getSubject();
    }

    public String extractUsernameFromRefreshToken(String token) {
        return Jwts.parser()
                .setSigningKey(Keys.hmacShaKeyFor(REFRESH_SECRET.getBytes()))
                .parseClaimsJws(token)
                .getBody()
                .getSubject();
    }

    public boolean validateAccessToken(String token) {
        try {
            Jwts.parser()
                    .setSigningKey(Keys.hmacShaKeyFor(ACCESS_SECRET.getBytes()))
                    .parseClaimsJws(token);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public boolean validateRefreshToken(String token) {
        try {
            Jwts.parser()
                    .setSigningKey(Keys.hmacShaKeyFor(REFRESH_SECRET.getBytes()))
                    .parseClaimsJws(token);
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}