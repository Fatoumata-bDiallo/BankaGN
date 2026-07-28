package com.bankagn.bankagn.controller;
import com.bankagn.bankagn.dto.DepotRetraitRequest;
import com.bankagn.bankagn.dto.TransfertRequest;

import com.bankagn.bankagn.dto.DashboardResponse;
import com.bankagn.bankagn.entity.Compte;
import com.bankagn.bankagn.entity.Transaction;
import com.bankagn.bankagn.entity.Utilisateur;
import com.bankagn.bankagn.repository.NotificationRepository;
import com.bankagn.bankagn.repository.UtilisateurRepository;
import com.bankagn.bankagn.service.impl.CompteService;
import com.bankagn.bankagn.service.impl.TransactionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/client")
@RequiredArgsConstructor
public class ClientApiController {

    private final UtilisateurRepository utilisateurRepository;
    private final CompteService compteService;
    private final TransactionService transactionService;
    private final NotificationRepository notificationRepository;

    @GetMapping("/dashboard")
    public ResponseEntity<DashboardResponse> dashboard(
            Authentication authentication) {

        String email = authentication.getName();
        Utilisateur utilisateur = utilisateurRepository
                .findByEmail(email).orElseThrow();

        List<Compte> comptes = compteService.getComptesByEmail(email);

        BigDecimal soldeTotal = comptes.stream()
                .map(Compte::getSolde)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal soldeCourant = comptes.stream()
                .filter(c -> c.getType() == Compte.TypeCompte.COURANT)
                .map(Compte::getSolde)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal soldeEpargne = comptes.stream()
                .filter(c -> c.getType() == Compte.TypeCompte.EPARGNE)
                .map(Compte::getSolde)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        long notificationsNonLues = notificationRepository
                .countByUtilisateurAndLu(utilisateur, false);

        List<Transaction> transactions = transactionService
                .getDernieresTransactions(email);

        DashboardResponse response = new DashboardResponse(
                utilisateur.getPrenom(),
                utilisateur.getNom(),
                notificationsNonLues,
                comptes,
                soldeTotal,
                soldeCourant,
                soldeEpargne,
                transactions
        );

        return ResponseEntity.ok(response);
    }

    @PostMapping("/comptes/creer")
    public ResponseEntity<Map<String, String>> creerCompte(
            @RequestBody Map<String, String> body,
            Authentication authentication) {

        String email = authentication.getName();
        String type = body.get("type");

        try {
            compteService.creerCompte(email, type);
            return ResponseEntity.ok(
                    Map.of("message", "Compte " + type + " créé avec succès !"));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("message", e.getMessage()));
        }
    }
    @PostMapping("/depot")
    public ResponseEntity<Map<String, String>> depot(
            @RequestBody DepotRetraitRequest request) {
        try {
            transactionService.effectuerDepot(
                    request.getCompteId(),
                    request.getMontant(),
                    request.getDescription());
            return ResponseEntity.ok(
                    Map.of("message", "Dépôt effectué avec succès !"));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("message", e.getMessage()));
        }
    }

    @PostMapping("/retrait")
    public ResponseEntity<Map<String, String>> retrait(
            @RequestBody DepotRetraitRequest request) {
        try {
            transactionService.effectuerRetrait(
                    request.getCompteId(),
                    request.getMontant(),
                    request.getDescription());
            return ResponseEntity.ok(
                    Map.of("message", "Retrait effectué avec succès !"));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("message", e.getMessage()));
        }
    }

    @PostMapping("/transfert")
    public ResponseEntity<Map<String, String>> transfert(
            @RequestBody TransfertRequest request) {
        try {
            transactionService.effectuerTransfert(
                    request.getCompteSourceId(),
                    request.getNumeroDestination(),
                    request.getMontant(),
                    request.getDescription());
            return ResponseEntity.ok(
                    Map.of("message", "Transfert effectué avec succès !"));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("message", e.getMessage()));
        }
    }

}