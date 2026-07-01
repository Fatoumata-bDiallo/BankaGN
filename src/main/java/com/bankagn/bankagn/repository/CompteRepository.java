package com.bankagn.bankagn.repository;

import com.bankagn.bankagn.entity.Compte;
import com.bankagn.bankagn.entity.Utilisateur;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CompteRepository
        extends JpaRepository<Compte, Long> {

    List<Compte> findByUtilisateur(Utilisateur utilisateur);

    List<Compte> findByUtilisateurAndType(
            Utilisateur utilisateur,
            Compte.TypeCompte type);

    long countByType(Compte.TypeCompte type);

    boolean existsByNumeroCompte(String numeroCompte);

    Optional<Compte> findByNumeroCompte(String numeroCompte);
}