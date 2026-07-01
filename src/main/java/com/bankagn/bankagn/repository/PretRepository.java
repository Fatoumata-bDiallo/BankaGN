package com.bankagn.bankagn.repository;

import com.bankagn.bankagn.entity.Pret;
import com.bankagn.bankagn.entity.Utilisateur;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PretRepository
        extends JpaRepository<Pret, Long> {

    List<Pret> findByUtilisateur(Utilisateur utilisateur);

    List<Pret> findByStatut(Pret.StatutPret statut);

    long countByStatut(Pret.StatutPret statut);
}