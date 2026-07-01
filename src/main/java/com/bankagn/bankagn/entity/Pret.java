package com.bankagn.bankagn.entity;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "prets")
@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Pret {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String reference;

    @Column(nullable = false)
    private BigDecimal montant;

    @Column(nullable = false)
    private BigDecimal tauxInteret;

    @Column(nullable = false)
    private Integer dureeMois;

    private BigDecimal mensualite;

    private BigDecimal montantTotal;

    @Column(columnDefinition = "TEXT")
    private String motif;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private StatutPret statut = StatutPret.EN_ATTENTE;

    private LocalDateTime dateCreation;

    private LocalDateTime dateDecision;

    @ManyToOne
    @JoinColumn(name = "utilisateur_id", nullable = false)
    private Utilisateur utilisateur;

    @ManyToOne
    @JoinColumn(name = "compte_id")
    private Compte compte;

    @PrePersist
    public void prePersist() {
        this.dateCreation = LocalDateTime.now();
    }

    public enum StatutPret {
        EN_ATTENTE, ACCEPTE, REFUSE, REMBOURSE
    }
}