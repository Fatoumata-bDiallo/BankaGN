package com.bankagn.bankagn.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "notifications")
@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Notification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String titre;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String message;

    @Enumerated(EnumType.STRING)
    private TypeNotification type;

    private boolean lu = false;

    private LocalDateTime dateCreation;

    @JsonIgnore
    @ManyToOne
    @JoinColumn(name = "utilisateur_id", nullable = false)
    private Utilisateur utilisateur;

    @PrePersist
    public void prePersist() {
        this.dateCreation = LocalDateTime.now();
    }

    public enum TypeNotification {
        DEPOT, RETRAIT, TRANSFERT,
        PRET, CONNEXION, ALERTE, SYSTEME
    }
}