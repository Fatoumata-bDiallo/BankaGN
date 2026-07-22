package com.bankagn.bankagn.entity;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "transactions")
@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Transaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String reference;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TypeTransaction type;

    @Column(nullable = false)
    private BigDecimal montant;

    private String devise = "GNF";

    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private StatutTransaction statut = StatutTransaction.SUCCES;

    private LocalDateTime dateTransaction;

    @Column(columnDefinition = "TEXT")
    private String qrCode;

    @ManyToOne
    @JoinColumn(name = "compte_source_id")
    private Compte compteSource;

    @ManyToOne
    @JoinColumn(name = "compte_destination_id")
    private Compte compteDestination;

    @PrePersist
    public void prePersist() {
        this.dateTransaction = LocalDateTime.now();
    }

    public enum TypeTransaction {
        DEPOT, RETRAIT, TRANSFERT, INTERET, FRAIS
    }

    public enum StatutTransaction {
        SUCCES, ECHEC, EN_ATTENTE
    }
}