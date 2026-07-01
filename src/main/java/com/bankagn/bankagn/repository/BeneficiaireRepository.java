package com.bankagn.bankagn.repository;

import com.bankagn.bankagn.entity.Beneficiaire;
import com.bankagn.bankagn.entity.Utilisateur;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface BeneficiaireRepository
        extends JpaRepository<Beneficiaire, Long> {

    List<Beneficiaire> findByUtilisateur(
            Utilisateur utilisateur);

    boolean existsByUtilisateurAndNumeroCompte(
            Utilisateur utilisateur, String numeroCompte);
}