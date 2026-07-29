package com.bankagn.bankagn.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.math.BigDecimal;

@Getter
@AllArgsConstructor
public class RapportResponse {
    private long totalClients;
    private long clientsActifs;
    private long totalComptes;
    private long totalTransactions;
    private BigDecimal totalDepots;
    private BigDecimal totalRetraits;
    private BigDecimal totalTransferts;
    private BigDecimal totalSoldes;
    private long enAttente;
    private long alertesNonResolues;
}