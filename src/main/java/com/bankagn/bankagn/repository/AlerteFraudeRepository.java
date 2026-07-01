package com.bankagn.bankagn.repository;

import com.bankagn.bankagn.entity.AlerteFraude;
import com.bankagn.bankagn.entity.Utilisateur;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AlerteFraudeRepository
        extends JpaRepository<AlerteFraude, Long> {

    List<AlerteFraude> findByUtilisateur(
            Utilisateur utilisateur);

    List<AlerteFraude> findByStatut(
            AlerteFraude.StatutAlerteEnum statut);

    long countByStatut(
            AlerteFraude.StatutAlerteEnum statut);
}