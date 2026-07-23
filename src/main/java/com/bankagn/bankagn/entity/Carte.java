package com.bankagn.bankagn.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "cartes")
@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Carte {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String numeroCarte;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TypeCarte type;

    @Column(nullable = false)
    private String cvv;

    @Column(nullable = false)
    private LocalDate dateExpiration;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private StatutCarte statut = StatutCarte.ACTIVE;

    private BigDecimal plafond = new BigDecimal("2000000");

    private LocalDateTime dateCreation;

    @JsonIgnore
    @ManyToOne
    @JoinColumn(name = "compte_id", nullable = false)
    private Compte compte;

    @PrePersist
    public void prePersist() {
        this.dateCreation = LocalDateTime.now();
    }

    public enum TypeCarte {
        VISA, MASTERCARD, VIRTUELLE
    }

    public enum StatutCarte {
        ACTIVE, BLOQUEE, EXPIREE
    }
}