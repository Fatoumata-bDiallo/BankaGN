package com.bankagn.bankagn.dto;

import com.bankagn.bankagn.entity.Transaction;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.math.BigDecimal;
import java.util.List;

@Getter
@AllArgsConstructor
public class DashboardResponse {
    private String prenom;
    private BigDecimal soldeTotal;
    private BigDecimal soldeCourant;
    private BigDecimal soldeEpargne;
    private long notificationsNonLues;
    private List<Transaction> transactions;
}