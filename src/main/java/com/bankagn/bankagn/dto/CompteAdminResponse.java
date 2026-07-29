package com.bankagn.bankagn.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@AllArgsConstructor
public class CompteAdminResponse {
    private Long id;
    private String numeroCompte;
    private String client;
    private String type;
    private BigDecimal solde;
    private String statut;
    private LocalDateTime dateCreation;
}