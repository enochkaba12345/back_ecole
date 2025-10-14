package com.sysgepecole.demo.Security;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;

import jakarta.annotation.PostConstruct;
import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.util.Date;
import java.util.stream.Collectors;

@Component
public class JwtProvider {

    private static final Logger logger = LoggerFactory.getLogger(JwtProvider.class);

    @Value("${jwt.secret}")
    private String jwtSecret;

    @Value("${jwt.expiration}")
    private Long jwtExpiration;

    private Key key;

    // ✅ Cette méthode est exécutée après l’injection des propriétés @Value
    @PostConstruct
    public void init() {
        this.key = Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));
    }

    // ✅ Génération du token depuis un utilisateur authentifié
    public String generateJwtToken(Authentication authentication) {
        UserSecurityModel userPrincipal = (UserSecurityModel) authentication.getPrincipal();

        return Jwts.builder()
                .setSubject(userPrincipal.getUsername())
                .claim("roles", userPrincipal.getAuthorities().stream()
                        .map(GrantedAuthority::getAuthority)
                        .collect(Collectors.toList()))
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + jwtExpiration))
                .signWith(key, SignatureAlgorithm.HS512)
                .compact();
    }

    // ✅ Génération simple d’un token à partir d’un nom d’utilisateur
    public String generateToken(String username) {
        return Jwts.builder()
                .setSubject(username)
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + jwtExpiration))
                .signWith(key, SignatureAlgorithm.HS512)
                .compact();
    }

    // ✅ Validation du token
    public boolean validateJwtToken(String token) {
        if (token == null || token.isEmpty()) {
            logger.error("JWT String argument cannot be null or empty.");
            return false;
        }

        try {
            Jwts.parserBuilder()
                    .setSigningKey(key)
                    .build()
                    .parseSignedClaims(token);
            return true;
        } catch (ExpiredJwtException e) {
            logger.error("JWT token is expired: {}", e.getMessage());
        } catch (UnsupportedJwtException e) {
            logger.error("JWT token is unsupported: {}", e.getMessage());
        } catch (MalformedJwtException e) {
            logger.error("Invalid JWT token: {}", e.getMessage());
        } catch (SignatureException e) {
            logger.error("Invalid JWT signature: {}", e.getMessage());
        } catch (IllegalArgumentException e) {
            logger.error("JWT claims string is empty: {}", e.getMessage());
        } catch (JwtException e) {
            logger.error("JWT validation error: {}", e.getMessage());
        }

        return false;
    }

    // ✅ Extraction du username depuis le token
    public String getUserNameFromJwtToken(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(key)
                .build()
                .parseSignedClaims(token)
                .getPayload()
                .getSubject();
    }

    // ✅ Vérification rapide
    public boolean isTokenValid(String token) {
        try {
            Jwts.parserBuilder().setSigningKey(key).build().parseSignedClaims(token);
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}
