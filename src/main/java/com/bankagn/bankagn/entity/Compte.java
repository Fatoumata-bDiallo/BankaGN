package com.bankagn.bankagn.entity;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "comptes")
@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Compte {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String numeroCompte;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TypeCompte type;

    @Column(nullable = false)
    private BigDecimal solde = BigDecimal.ZERO;

    private BigDecimal tauxInteret = BigDecimal.ZERO;

    private BigDecimal plafondRetrait = new BigDecimal("5000000");

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private StatutCompte statut = StatutCompte.ACTIF;

    @Column(length = 500)
    private String qrCode;

    private LocalDateTime dateCreation;

    @ManyToOne
    @JoinColumn(name = "utilisateur_id", nullable = false)
    private Utilisateur utilisateur;

    @OneToMany(mappedBy = "compteSource",
            cascade = CascadeType.ALL)
    private List<Transaction> transactionsSource;

    @OneToMany(mappedBy = "compteDestination",
            cascade = CascadeType.ALL)
    private List<Transaction> transactionsDestination;

    @OneToMany(mappedBy = "compte",
            cascade = CascadeType.ALL)
    private List<Carte> cartes;

    @PrePersist
    public void prePersist() {
        this.dateCreation = LocalDateTime.now();
    }

    public enum TypeCompte {
        COURANT, EPARGNE
    }

    public enum StatutCompte {
        ACTIF, BLOQUE, FERME
    }
}