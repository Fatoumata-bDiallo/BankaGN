package com.bankagn.bankagn.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@AllArgsConstructor
public class AlerteFraudeResponse {
    private Long id;
    private String typeAlerte;
    private String client;
    private String niveau;
    private String statut;
    private boolean resolu;
    private LocalDateTime dateAlerte;
}