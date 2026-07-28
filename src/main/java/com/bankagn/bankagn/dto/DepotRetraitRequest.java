package com.bankagn.bankagn.dto;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
public class DepotRetraitRequest {
    private Long compteId;
    private BigDecimal montant;
    private String description;
}