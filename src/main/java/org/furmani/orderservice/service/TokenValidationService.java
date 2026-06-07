package org.furmani.orderservice.service;

import org.furmani.orderservice.security.AuthenticatedUser;

public interface TokenValidationService {
    AuthenticatedUser validateToken(String token);
}


