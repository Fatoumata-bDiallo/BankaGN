package com.bankagn.bankagn.controller;

import com.bankagn.bankagn.dto.ApiResponse;
import com.bankagn.bankagn.dto.DashboardResponse;
import com.bankagn.bankagn.entity.Compte;
import com.bankagn.bankagn.entity.Transaction;
import com.bankagn.bankagn.entity.Utilisateur;
import com.bankagn.bankagn.repository.NotificationRepository;
import com.bankagn.bankagn.repository.UtilisateurRepository;
import com.bankagn.bankagn.service.impl.CompteService;
import com.bankagn.bankagn.service.impl.TransactionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/api/client")
@RequiredArgsConstructor
public class ClientApiController {

    private final UtilisateurRepository utilisateurRepository;
    private final CompteService compteService;
    private final TransactionService transactionService;
    private final NotificationRepository notificationRepository;

    @GetMapping("/dashboard")
    public ResponseEntity<ApiResponse<DashboardResponse>> dashboard(
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
                soldeTotal,
                soldeCourant,
                soldeEpargne,
                notificationsNonLues,
                transactions
        );

        return ResponseEntity.ok(ApiResponse.ok("OK", response));
    }
}