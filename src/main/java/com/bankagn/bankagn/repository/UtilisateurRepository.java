package com.bankagn.bankagn.repository;

import com.bankagn.bankagn.entity.Utilisateur;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UtilisateurRepository
        extends JpaRepository<Utilisateur, Long> {

    // Trouver par email
    Optional<Utilisateur> findByEmail(String email);

    // Vérifier si email existe
    boolean existsByEmail(String email);

    // Trouver par statut
    java.util.List<Utilisateur> findByStatut(
            Utilisateur.Statut statut);

    // Compter les clients
    long countByRole(Utilisateur.Role role);
}