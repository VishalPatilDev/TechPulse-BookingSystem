package com.techpulse.booking_system.util;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Date;

@Component
public class JwtUtil {
    private final String secreate ="This-is-my-jwt-secrete-key-so-that-jwt-can-understand-it-is-of-32-bit";
    private final SecretKey key = Keys.hmacShaKeyFor(secreate.getBytes());

    public String generateToken(String username){
        return Jwts.builder()
                .subject(username)
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis()+1000*60*60))
                .signWith(key)
                .compact();
    }

    private Claims extractClaims(String token){
        return Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token.trim())
                .getPayload();
    }
    public String extractUsername(String token){
        Claims payload =extractClaims(token);
        return payload.getSubject();
    }
    public boolean validateToken(UserDetails userDetails, String username, String token){
        return username.equals(userDetails.getUsername()) &&  !isTokenExpired(token);
    }
    private boolean isTokenExpired(String token){
        return extractClaims(token).getExpiration().before(new Date());
    }
}
