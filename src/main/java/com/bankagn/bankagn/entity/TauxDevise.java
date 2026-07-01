package com.bankagn.bankagn.entity;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "taux_devises")
@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TauxDevise {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String code;

    @Column(nullable = false)
    private String nom;

    @Column(nullable = false)
    private String drapeau;

    @Column(nullable = false, precision = 15, scale = 8)
    private BigDecimal tauxVersGNF;

    private LocalDateTime derniereMiseAJour;

    @PrePersist
    @PreUpdate
    public void preUpdate() {
        this.derniereMiseAJour = LocalDateTime.now();
    }
}