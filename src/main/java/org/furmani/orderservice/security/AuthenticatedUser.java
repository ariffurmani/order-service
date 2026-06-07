package org.furmani.orderservice.security;

import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
public class AuthenticatedUser {
    private String email;
    private List<String> roles = new ArrayList<>();
}

