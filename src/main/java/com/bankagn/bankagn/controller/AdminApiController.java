package com.bankagn.bankagn.controller;
import com.bankagn.bankagn.dto.AlerteFraudeResponse;
import com.bankagn.bankagn.dto.CompteAdminResponse;
import com.bankagn.bankagn.dto.PretAdminResponse;
import com.bankagn.bankagn.dto.RapportResponse;
import com.bankagn.bankagn.entity.AlerteFraude;
import com.bankagn.bankagn.entity.Compte;
import com.bankagn.bankagn.entity.Pret;
import com.bankagn.bankagn.entity.Transaction;
import com.bankagn.bankagn.entity.Utilisateur;
import com.bankagn.bankagn.repository.AlerteFraudeRepository;
import com.bankagn.bankagn.repository.CompteRepository;
import com.bankagn.bankagn.repository.PretRepository;
import com.bankagn.bankagn.repository.TransactionRepository;
import java.math.BigDecimal;
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
import com.bankagn.bankagn.service.impl.PretService;
import org.springframework.http.HttpStatus;
import com.bankagn.bankagn.entity.TauxDevise;
import com.bankagn.bankagn.repository.TauxDeviseRepository;
import com.bankagn.bankagn.service.impl.CompteService;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminApiController {

    private final UtilisateurRepository utilisateurRepository;
    private final NotificationRepository notificationRepository;
    private final JournalAuditRepository journalAuditRepository;
    private final CompteRepository compteRepository;
    private final TransactionRepository transactionRepository;
    private final PretRepository pretRepository;
    private final AlerteFraudeRepository alerteFraudeRepository;
    private final PretService pretService;
    private final TauxDeviseRepository tauxDeviseRepository;
    private final CompteService compteService;

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
    @GetMapping("/comptes")
    public ResponseEntity<List<CompteAdminResponse>> comptes() {
        List<CompteAdminResponse> result = compteRepository.findAll().stream()
                .map(c -> new CompteAdminResponse(
                        c.getId(),
                        c.getNumeroCompte(),
                        c.getUtilisateur().getPrenom() + " " + c.getUtilisateur().getNom(),
                        c.getType().name(),
                        c.getSolde(),
                        c.getStatut().name(),
                        c.getDateCreation()))
                .toList();
        return ResponseEntity.ok(result);
    }

    @GetMapping("/transactions")
    public ResponseEntity<List<Transaction>> transactions() {
        return ResponseEntity.ok(transactionRepository.findAll());
    }

    @GetMapping("/prets")
    public ResponseEntity<List<PretAdminResponse>> prets() {
        List<PretAdminResponse> result = pretRepository.findAll().stream()
                .map(p -> new PretAdminResponse(
                        p.getId(),
                        p.getReference(),
                        p.getUtilisateur().getPrenom() + " " + p.getUtilisateur().getNom(),
                        p.getMontant(),
                        p.getStatut().name(),
                        p.getDateCreation()))
                .toList();
        return ResponseEntity.ok(result);
    }

    @GetMapping("/fraudes")
    public ResponseEntity<List<AlerteFraudeResponse>> fraudes() {
        List<AlerteFraudeResponse> result = alerteFraudeRepository.findAll().stream()
                .map(a -> new AlerteFraudeResponse(
                        a.getId(),
                        a.getTypeAlerte(),
                        a.getUtilisateur().getPrenom() + " " + a.getUtilisateur().getNom(),
                        a.getNiveau().name(),
                        a.getStatut().name(),
                        a.isResolu(),
                        a.getDateAlerte()))
                .toList();
        return ResponseEntity.ok(result);
    }

    @GetMapping("/audit")
    public ResponseEntity<List<JournalAudit>> audit() {
        return ResponseEntity.ok(journalAuditRepository.findAll());
    }

    @GetMapping("/dashboard")
    public ResponseEntity<RapportResponse> dashboard() {
        return ResponseEntity.ok(construireRapport());
    }

    @GetMapping("/rapports")
    public ResponseEntity<RapportResponse> rapports() {
        return ResponseEntity.ok(construireRapport());
    }

    private RapportResponse construireRapport() {
        long totalClients = utilisateurRepository.findAll().stream()
                .filter(u -> u.getRole() == Utilisateur.Role.CLIENT)
                .count();

        long clientsActifs = utilisateurRepository.findAll().stream()
                .filter(u -> u.getRole() == Utilisateur.Role.CLIENT
                        && u.getStatut() == Utilisateur.Statut.ACTIF)
                .count();

        long enAttente = utilisateurRepository.findAll().stream()
                .filter(u -> u.getRole() == Utilisateur.Role.CLIENT
                        && u.getStatut() == Utilisateur.Statut.EN_ATTENTE)
                .count();

        long totalComptes = compteRepository.count();

        BigDecimal totalSoldes = compteRepository.findAll().stream()
                .map(Compte::getSolde)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        List<Transaction> transactions = transactionRepository.findAll();
        long totalTransactions = transactions.size();

        BigDecimal totalDepots = transactions.stream()
                .filter(t -> t.getType() == Transaction.TypeTransaction.DEPOT)
                .map(Transaction::getMontant)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalRetraits = transactions.stream()
                .filter(t -> t.getType() == Transaction.TypeTransaction.RETRAIT)
                .map(Transaction::getMontant)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalTransferts = transactions.stream()
                .filter(t -> t.getType() == Transaction.TypeTransaction.TRANSFERT)
                .map(Transaction::getMontant)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        long alertesNonResolues = alerteFraudeRepository
                .countByStatut(AlerteFraude.StatutAlerteEnum.EN_COURS);

        return new RapportResponse(
                totalClients, clientsActifs, totalComptes, totalTransactions,
                totalDepots, totalRetraits, totalTransferts, totalSoldes,
                enAttente, alertesNonResolues);
    }
    @PutMapping("/prets/{id}/accepter")
    public ResponseEntity<Map<String, String>> accepterPret(@PathVariable Long id) {
        try {
            pretService.accepterPret(id);
            return ResponseEntity.ok(Map.of("message", "Prêt accepté !"));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("message", e.getMessage()));
        }
    }

    @PutMapping("/prets/{id}/refuser")
    public ResponseEntity<Map<String, String>> refuserPret(@PathVariable Long id) {
        try {
            pretService.refuserPret(id);
            return ResponseEntity.ok(Map.of("message", "Prêt refusé !"));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("message", e.getMessage()));
        }
    }
    @GetMapping("/devises")
    public ResponseEntity<List<TauxDevise>> devises() {
        return ResponseEntity.ok(tauxDeviseRepository.findAll());
    }

    @PutMapping("/devises/{id}")
    public ResponseEntity<Map<String, String>> modifierDevise(
            @PathVariable Long id,
            @RequestBody Map<String, String> body,
            Authentication authentication) {
        try {
            TauxDevise taux = tauxDeviseRepository.findById(id).orElseThrow();
            BigDecimal ancien = taux.getTauxVersGNF();
            BigDecimal nouveau = new BigDecimal(body.get("tauxVersGNF"));
            taux.setTauxVersGNF(nouveau);
            tauxDeviseRepository.save(taux);

            journalAuditRepository.save(JournalAudit.builder()
                    .action("Taux de change modifié")
                    .details("Taux " + taux.getCode() + " modifié de "
                            + ancien + " GNF → " + nouveau + " GNF")
                    .effectuePar(authentication.getName())
                    .typeAction(JournalAudit.TypeAction.SYSTEME)
                    .build());

            return ResponseEntity.ok(Map.of("message",
                    "Taux " + taux.getCode() + " mis à jour : " + nouveau + " GNF !"));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("message", "Erreur lors de la mise à jour !"));
        }
    }

    @PutMapping("/comptes/{id}/bloquer")
    public ResponseEntity<Map<String, String>> bloquerCompte(@PathVariable Long id) {
        compteService.bloquerCompte(id);
        return ResponseEntity.ok(Map.of("message", "Compte bloqué !"));
    }

    @PutMapping("/comptes/{id}/debloquer")
    public ResponseEntity<Map<String, String>> debloquerCompte(@PathVariable Long id) {
        compteService.debloquerCompte(id);
        return ResponseEntity.ok(Map.of("message", "Compte débloqué !"));
    }
}