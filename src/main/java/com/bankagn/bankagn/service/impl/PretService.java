package com.bankagn.bankagn.service.impl;

import com.bankagn.bankagn.entity.Compte;
import com.bankagn.bankagn.entity.Notification;
import com.bankagn.bankagn.entity.Pret;
import com.bankagn.bankagn.entity.Utilisateur;
import com.bankagn.bankagn.repository.CompteRepository;
import com.bankagn.bankagn.repository.NotificationRepository;
import com.bankagn.bankagn.repository.PretRepository;
import com.bankagn.bankagn.repository.UtilisateurRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Random;

@Service
@RequiredArgsConstructor
public class PretService {

    private final PretRepository pretRepository;
    private final UtilisateurRepository utilisateurRepository;
    private final CompteRepository compteRepository;
    private final NotificationRepository notificationRepository;

    // Demander un prêt
    @Transactional
    public Pret demanderPret(String email,
                             BigDecimal montant,
                             Integer dureeMois,
                             BigDecimal tauxInteret,
                             String motif,
                             Long compteId) {
        Utilisateur utilisateur = utilisateurRepository
                .findByEmail(email).orElseThrow();

        // Calcul mensualité
        BigDecimal tauxMensuel = tauxInteret
                .divide(new BigDecimal("1200"), 10,
                        RoundingMode.HALF_UP);
        BigDecimal mensualite = calculerMensualite(
                montant, tauxMensuel, dureeMois);
        BigDecimal montantTotal = mensualite
                .multiply(new BigDecimal(dureeMois));

        Compte compte = compteId != null ?
                compteRepository.findById(compteId)
                        .orElse(null) : null;

        Pret pret = Pret.builder()
                .reference("PRE-" + System.currentTimeMillis())
                .montant(montant)
                .tauxInteret(tauxInteret)
                .dureeMois(dureeMois)
                .mensualite(mensualite)
                .montantTotal(montantTotal)
                .motif(motif)
                .statut(Pret.StatutPret.EN_ATTENTE)
                .utilisateur(utilisateur)
                .compte(compte)
                .build();

        pretRepository.save(pret);

        // Notification
        Notification notification = Notification.builder()
                .titre("Demande de prêt envoyée")
                .message("Votre demande de prêt de " +
                        montant + " GNF est en cours d'examen.")
                .type(Notification.TypeNotification.PRET)
                .lu(false)
                .utilisateur(utilisateur)
                .build();
        notificationRepository.save(notification);

        return pret;
    }

    // Récupérer les prêts d'un client
    public List<Pret> getPretsByEmail(String email) {
        Utilisateur utilisateur = utilisateurRepository
                .findByEmail(email).orElseThrow();
        return pretRepository.findByUtilisateur(utilisateur);
    }

    // Tous les prêts (admin)
    public List<Pret> getAllPrets() {
        return pretRepository.findAll();
    }

    // Accepter un prêt (admin)
    @Transactional
    public void accepterPret(Long id) {
        Pret pret = pretRepository.findById(id).orElseThrow();
        pret.setStatut(Pret.StatutPret.ACCEPTE);

        // Créditer le compte
        if (pret.getCompte() != null) {
            Compte compte = pret.getCompte();
            compte.setSolde(compte.getSolde()
                    .add(pret.getMontant()));
            compteRepository.save(compte);
        }

        pretRepository.save(pret);

        // Notification
        Notification notification = Notification.builder()
                .titre("Prêt accepté !")
                .message("Votre prêt de " + pret.getMontant() +
                        " GNF a été accepté et crédité.")
                .type(Notification.TypeNotification.PRET)
                .lu(false)
                .utilisateur(pret.getUtilisateur())
                .build();
        notificationRepository.save(notification);
    }

    // Refuser un prêt (admin)
    @Transactional
    public void refuserPret(Long id) {
        Pret pret = pretRepository.findById(id).orElseThrow();
        pret.setStatut(Pret.StatutPret.REFUSE);
        pretRepository.save(pret);

        Notification notification = Notification.builder()
                .titre("Prêt refusé")
                .message("Votre demande de prêt de " +
                        pret.getMontant() + " GNF a été refusée.")
                .type(Notification.TypeNotification.PRET)
                .lu(false)
                .utilisateur(pret.getUtilisateur())
                .build();
        notificationRepository.save(notification);
    }

    // Calcul mensualité
    private BigDecimal calculerMensualite(BigDecimal montant,
                                          BigDecimal tauxMensuel,
                                          int duree) {
        if (tauxMensuel.compareTo(BigDecimal.ZERO) == 0) {
            return montant.divide(new BigDecimal(duree),
                    2, RoundingMode.HALF_UP);
        }
        double m = montant.doubleValue();
        double t = tauxMensuel.doubleValue();
        double n = duree;
        double mensualite = m * t * Math.pow(1 + t, n)
                / (Math.pow(1 + t, n) - 1);
        return new BigDecimal(mensualite)
                .setScale(2, RoundingMode.HALF_UP);
    }
}