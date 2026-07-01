package com.bankagn.bankagn.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "journal_audit")
@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class JournalAudit {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String action;

    @Column(nullable = false)
    private String details;

    @Column(nullable = false)
    private String effectuePar;

    @Enumerated(EnumType.STRING)
    private TypeAction typeAction;

    private LocalDateTime dateAction;

    @PrePersist
    public void prePersist() {
        this.dateAction = LocalDateTime.now();
    }

    public enum TypeAction {
        CONNEXION, DECONNEXION, TRANSACTION,
        COMPTE, CARTE, PRET, UTILISATEUR, SYSTEME
    }
}