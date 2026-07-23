package com.bankagn.bankagn.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "alertes_fraude")
@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AlerteFraude {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String typeAlerte;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private NiveauAlerte niveau = NiveauAlerte.MOYEN;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private StatutAlerteEnum statut = StatutAlerteEnum.EN_COURS;

    private boolean resolu = false;

    private LocalDateTime dateAlerte;

    @JsonIgnore
    @ManyToOne
    @JoinColumn(name = "utilisateur_id", nullable = false)
    private Utilisateur utilisateur;

    @PrePersist
    public void prePersist() {
        this.dateAlerte = LocalDateTime.now();
    }

    public enum NiveauAlerte {
        FAIBLE, MOYEN, ELEVE
    }

    public enum StatutAlerteEnum {
        EN_COURS, RESOLUE, IGNOREE
    }
}