package com.bankagn.bankagn.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "utilisateurs")
@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Utilisateur {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String nom;

    @Column(nullable = false)
    private String prenom;

    @Column(unique = true, nullable = false)
    private String email;

    @Column(nullable = false)
    private String motDePasse;

    private String telephone;

    private String photo;

    @Column(columnDefinition = "LONGTEXT")
    private String pieceIdentite;

    private String typePiece;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Role role = Role.CLIENT;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Statut statut = Statut.EN_ATTENTE;

    private int tentativesConnexion = 0;

    private LocalDateTime dateCreation;

    private LocalDateTime derniereConnexion;

    private String resetToken;

    private LocalDateTime resetTokenExpiry;

    private String otpCode;

    private LocalDateTime otpExpiry;

    @JsonIgnore
    @OneToMany(mappedBy = "utilisateur",
            cascade = CascadeType.ALL)
    private List<Compte> comptes;

    @JsonIgnore
    @OneToMany(mappedBy = "utilisateur",
            cascade = CascadeType.ALL)
    private List<Notification> notifications;

    @PrePersist
    public void prePersist() {
        this.dateCreation = LocalDateTime.now();
    }

    public enum Role {
        ADMIN, CLIENT
    }

    public enum Statut {
        EN_ATTENTE, ACTIF, BLOQUE, INACTIF
    }
}