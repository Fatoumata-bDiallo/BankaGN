package com.bankagn.bankagn.repository;

import com.bankagn.bankagn.entity.Carte;
import com.bankagn.bankagn.entity.Compte;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CarteRepository
        extends JpaRepository<Carte, Long> {

    List<Carte> findByCompte(Compte compte);

    List<Carte> findByCompteIn(List<Compte> comptes);

    boolean existsByNumeroCarte(String numeroCarte);
}