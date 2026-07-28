package com.bankagn.bankagn.controller;

import com.bankagn.bankagn.entity.JournalAudit;
import com.bankagn.bankagn.entity.Notification;
import com.bankagn.bankagn.entity.Utilisateur;
import com.bankagn.bankagn.repository.JournalAuditRepository;
import com.bankagn.bankagn.repository.NotificationRepository;
import com.bankagn.bankagn.repository.UtilisateurRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminApiController {

    private final UtilisateurRepository utilisateurRepository;
    private final NotificationRepository notificationRepository;
    private final JournalAuditRepository journalAuditRepository;

    @GetMapping("/utilisateurs")
    public ResponseEntity<List<Utilisateur>> utilisateurs() {
        List<Utilisateur> utilisateurs = utilisateurRepository.findAll()
                .stream()
                .filter(u -> u.getRole() == Utilisateur.Role.CLIENT)
                .toList();
        return ResponseEntity.ok(utilisateurs);
    }

    @PutMapping("/utilisateurs/{id}/valider")
    public ResponseEntity<Map<String, String>> valider(
            @PathVariable Long id,
            Authentication authentication) {

        Utilisateur u = utilisateurRepository.findById(id).orElseThrow();
        u.setStatut(Utilisateur.Statut.ACTIF);
        utilisateurRepository.save(u);

        notificationRepository.save(Notification.builder()
                .titre("✅ Compte activé !")
                .message("Félicitations " + u.getPrenom() +
                        " ! Votre compte BankaGN a été validé.")
                .type(Notification.TypeNotification.SYSTEME)
                .lu(false)
                .utilisateur(u)
                .build());

        journalAuditRepository.save(JournalAudit.builder()
                .action("Compte validé (API)")
                .details("Compte de " + u.getPrenom() + " " + u.getNom() +
                        " (" + u.getEmail() + ") validé")
                .effectuePar(authentication.getName())
                .typeAction(JournalAudit.TypeAction.UTILISATEUR)
                .build());

        return ResponseEntity.ok(Map.of("message",
                "Compte de " + u.getPrenom() + " " + u.getNom() + " validé !"));
    }

    @PutMapping("/utilisateurs/{id}/bloquer")
    public ResponseEntity<Map<String, String>> bloquer(
            @PathVariable Long id,
            Authentication authentication) {

        Utilisateur u = utilisateurRepository.findById(id).orElseThrow();
        u.setStatut(Utilisateur.Statut.BLOQUE);
        utilisateurRepository.save(u);

        journalAuditRepository.save(JournalAudit.builder()
                .action("Compte bloqué (API)")
                .details("Compte de " + u.getPrenom() + " " + u.getNom() +
                        " (" + u.getEmail() + ") bloqué")
                .effectuePar(authentication.getName())
                .typeAction(JournalAudit.TypeAction.UTILISATEUR)
                .build());

        return ResponseEntity.ok(Map.of("message",
                "Compte de " + u.getPrenom() + " " + u.getNom() + " bloqué !"));
    }
}