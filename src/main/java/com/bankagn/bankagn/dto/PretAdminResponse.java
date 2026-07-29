package com.bankagn.bankagn.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@AllArgsConstructor
public class PretAdminResponse {
    private Long id;
    private String reference;
    private String client;
    private BigDecimal montant;
    private String statut;
    private LocalDateTime dateCreation;
}