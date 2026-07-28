package com.bankagn.bankagn.dto;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
public class TransfertRequest {
    private Long compteSourceId;
    private String numeroDestination;
    private BigDecimal montant;
    private String description;
}