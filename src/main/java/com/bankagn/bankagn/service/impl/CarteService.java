package com.bankagn.bankagn.service.impl;

import com.bankagn.bankagn.entity.Carte;
import com.bankagn.bankagn.entity.Compte;
import com.bankagn.bankagn.repository.CarteRepository;
import com.bankagn.bankagn.repository.CompteRepository;
import com.bankagn.bankagn.repository.UtilisateurRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Random;

@Service
@RequiredArgsConstructor
public class CarteService {

    private final CarteRepository carteRepository;
    private final CompteRepository compteRepository;
    private final UtilisateurRepository utilisateurRepository;

    // Créer une carte
    @Transactional
    public Carte creerCarte(Long compteId,
                            Carte.TypeCarte type) {
        Compte compte = compteRepository
                .findById(compteId).orElseThrow();

        String numeroCarte = genererNumeroCarte();
        String cvv = genererCVV();
        LocalDate dateExpiration = LocalDate.now()
                .plusYears(3);

        Carte carte = Carte.builder()
                .numeroCarte(numeroCarte)
                .type(type)
                .cvv(cvv)
                .dateExpiration(dateExpiration)
                .statut(Carte.StatutCarte.ACTIVE)
                .compte(compte)
                .build();

        return carteRepository.save(carte);
    }

    // Récupérer les cartes d'un utilisateur
    public List<Carte> getCartesByEmail(String email) {
        var utilisateur = utilisateurRepository
                .findByEmail(email).orElseThrow();
        List<Compte> comptes = compteRepository
                .findByUtilisateur(utilisateur);
        return carteRepository.findByCompteIn(comptes);
    }

    // Bloquer une carte
    @Transactional
    public void bloquerCarte(Long id) {
        Carte carte = carteRepository
                .findById(id).orElseThrow();
        carte.setStatut(Carte.StatutCarte.BLOQUEE);
        carteRepository.save(carte);
    }

    // Débloquer une carte
    @Transactional
    public void debloquerCarte(Long id) {
        Carte carte = carteRepository
                .findById(id).orElseThrow();
        carte.setStatut(Carte.StatutCarte.ACTIVE);
        carteRepository.save(carte);
    }

    // Supprimer une carte
    @Transactional
    public void supprimerCarte(Long id) {
        carteRepository.deleteById(id);
    }

    // Générer numéro de carte unique
    private String genererNumeroCarte() {
        String numero;
        do {
            Random r = new Random();
            numero = String.format("%04d-%04d-%04d-%04d",
                    r.nextInt(9000) + 1000,
                    r.nextInt(9000) + 1000,
                    r.nextInt(9000) + 1000,
                    r.nextInt(9000) + 1000);
        } while (carteRepository
                .existsByNumeroCarte(numero));
        return numero;
    }

    // Générer CVV
    private String genererCVV() {
        Random r = new Random();
        return String.format("%03d",
                r.nextInt(900) + 100);
    }
}