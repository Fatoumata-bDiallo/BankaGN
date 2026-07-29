package com.bankagn.bankagn.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
import com.fasterxml.jackson.annotation.JsonIgnore;

@Entity
@Table(name = "beneficiaires")
@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Beneficiaire {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String nom;

    @Column(nullable = false)
    private String numeroCompte;

    private String telephone;

    private String description;

    private LocalDateTime dateCreation;

    @JsonIgnore
    @ManyToOne
    @JoinColumn(name = "utilisateur_id",
            nullable = false)
    private Utilisateur utilisateur;

    @PrePersist
    public void prePersist() {
        this.dateCreation = LocalDateTime.now();
    }
}