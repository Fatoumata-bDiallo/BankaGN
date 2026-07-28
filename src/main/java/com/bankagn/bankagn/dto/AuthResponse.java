package com.bankagn.bankagn.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class AuthResponse {
    private String token;
    private Long id;
    private String nom;
    private String prenom;
    private String email;
    private String role;
}