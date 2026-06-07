package org.furmani.orderservice.interceptors;

import org.furmani.orderservice.exception.ForbiddenAccessException;
import org.furmani.orderservice.exception.UnauthorizedAccessException;
import org.furmani.orderservice.security.AuthenticatedUser;
import org.furmani.orderservice.service.TokenValidationService;
import jakarta.annotation.Nonnull;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.Locale;

@Slf4j
@Component
public class ProductAuthorizationInterceptor implements HandlerInterceptor {

    private static final String AUTH_HEADER = "Authorization";
    private static final String BEARER_PREFIX = "Bearer ";
    public static final String AUTHENTICATED_USER_ATTRIBUTE = "authenticatedUser";

    private final TokenValidationService tokenValidationService;

    public ProductAuthorizationInterceptor(TokenValidationService tokenValidationService) {
        this.tokenValidationService = tokenValidationService;
    }

    @Override
    public boolean preHandle(@Nonnull HttpServletRequest request, @Nonnull HttpServletResponse response, @Nonnull Object handler) {
        String method = request.getMethod();

        if ("OPTIONS".equalsIgnoreCase(method)) {
            return true;
        }

        String token = extractToken(request.getHeader(AUTH_HEADER));
        AuthenticatedUser authenticatedUser = tokenValidationService.validateToken(token);
        request.setAttribute(AUTHENTICATED_USER_ATTRIBUTE, authenticatedUser);

        if (isReadOperation(method)) {
            return true;
        }

        if (!hasAdminRole(authenticatedUser)) {
            log.warn("Access denied for user {} on {} {}", authenticatedUser.getEmail(), method, request.getRequestURI());
            throw new ForbiddenAccessException("Admin role is required for this operation");
        }

        return true;
    }

    private String extractToken(String authorizationHeader) {
        if (authorizationHeader == null || authorizationHeader.trim().isEmpty()) {
            throw new UnauthorizedAccessException("Authorization token is required");
        }

        String value = authorizationHeader.trim();
        if (!value.regionMatches(true, 0, BEARER_PREFIX, 0, BEARER_PREFIX.length())) {
            throw new UnauthorizedAccessException("Authorization header must use Bearer scheme");
        }

        String token = value.substring(BEARER_PREFIX.length()).trim();
        if (token.isEmpty()) {
            throw new UnauthorizedAccessException("Authorization token is required");
        }
        return token;
    }

    private boolean isReadOperation(String method) {
        return "GET".equalsIgnoreCase(method) || "HEAD".equalsIgnoreCase(method);
    }

    private boolean hasAdminRole(AuthenticatedUser authenticatedUser) {
        if (authenticatedUser == null || authenticatedUser.getRoles() == null) {
            return false;
        }

        for (String role : authenticatedUser.getRoles()) {
            if (role == null) {
                continue;
            }
            String normalized = role.trim().toUpperCase(Locale.ROOT);
            if (normalized.startsWith("ROLE_")) {
                normalized = normalized.substring(5);
            }
            if ("ADMIN".equals(normalized)) {
                return true;
            }
        }
        return false;
    }
}