package org.furmani.orderservice.service;

import org.furmani.orderservice.exception.InvalidTokenException;
import org.furmani.orderservice.security.AuthenticatedUser;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
public class JwtTokenValidationService implements TokenValidationService {

    private final String secretKeyValue;

    public JwtTokenValidationService(@Value("${jwt.secret-key:}") String secretKeyValue) {
        this.secretKeyValue = secretKeyValue;
    }

    @Override
    public AuthenticatedUser validateToken(String token) throws InvalidTokenException {
        log.info("Validating token");

        if (token == null || token.trim().isEmpty()) {
            log.warn("Token validation failed: token is null or empty");
            throw new InvalidTokenException("Token cannot be null or empty");
        }

        if (secretKeyValue == null || secretKeyValue.trim().isEmpty()) {
            log.error("JWT secret key is not configured");
            throw new IllegalStateException("Authentication configuration is invalid");
        }

        try {
            Claims claims = Jwts.parser()
                    .verifyWith(buildSecretKey(secretKeyValue.trim()))
                    .build()
                    .parseSignedClaims(token.trim())
                    .getPayload();

            Date expiration = claims.getExpiration();
            if (expiration == null) {
                log.warn("Token validation failed: exp claim is missing");
                throw new InvalidTokenException("Token is missing expiry claim");
            }

            long currentDate = System.currentTimeMillis();
            if (expiration.getTime() <= currentDate) {
                log.warn("Token validation failed: token has expired. Expiry: {}, Current: {}", expiration.getTime(), currentDate);
                throw new InvalidTokenException("Token has expired");
            }

            String email = claims.get("email", String.class);
            if (email == null || email.trim().isEmpty()) {
                email = claims.getSubject();
            }
            if (email == null || email.trim().isEmpty()) {
                log.warn("Token validation failed: email claim is missing");
                throw new InvalidTokenException("Token is missing email claim");
            }

            AuthenticatedUser authUser = new AuthenticatedUser();
            authUser.setEmail(email.trim());
            authUser.setRoles(extractRoles(claims.get("roles")));

            log.info("Token validation successful for user: {}", email);
            return authUser;
        } catch (InvalidTokenException e) {
            throw e;
        } catch (JwtException e) {
            log.warn("Token validation failed: JWT parsing error", e);
            throw new InvalidTokenException("Invalid token", e);
        } catch (Exception e) {
            log.error("An unexpected error occurred during token validation", e);
            throw new InvalidTokenException("Token validation failed", e);
        }
    }

    private List<String> extractRoles(Object rolesObject) {
        List<String> roleNames = new ArrayList<>();
        if (rolesObject instanceof List<?> rolesList) {
            for (Object role : rolesList) {
                if (role instanceof Map<?, ?> roleMap) {
                    Object value = roleMap.get("value");
                    if (value != null) {
                        roleNames.add(value.toString());
                    }
                } else if (role != null) {
                    roleNames.add(role.toString());
                }
            }
        } else if (rolesObject instanceof String rolesAsString) {
            for (String role : rolesAsString.split(",")) {
                String trimmed = role.trim();
                if (!trimmed.isEmpty()) {
                    roleNames.add(trimmed);
                }
            }
        }
        return roleNames;
    }

    private SecretKey buildSecretKey(String secretKeyValue) {
        try {
            byte[] decoded = Decoders.BASE64.decode(secretKeyValue);
            return Keys.hmacShaKeyFor(decoded);
        } catch (IllegalArgumentException ignored) {
            return Keys.hmacShaKeyFor(secretKeyValue.getBytes(StandardCharsets.UTF_8));
        }
    }
}


