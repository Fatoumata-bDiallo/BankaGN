package com.bankagn.bankagn.controller;

import com.bankagn.bankagn.entity.Compte;
import com.bankagn.bankagn.entity.Utilisateur;
import com.bankagn.bankagn.repository.NotificationRepository;
import com.bankagn.bankagn.repository.UtilisateurRepository;
import com.bankagn.bankagn.service.impl.CompteService;
import com.bankagn.bankagn.service.impl.TransactionService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.math.BigDecimal;
import java.util.List;

@Controller
@RequestMapping("/client")
@RequiredArgsConstructor
public class ClientController {

    private final UtilisateurRepository utilisateurRepository;
    private final CompteService compteService;
    private final TransactionService transactionService;
    private final NotificationRepository notificationRepository;

    @GetMapping("/dashboard")
    public String dashboard(Authentication authentication,
                            Model model) {

        String email = authentication.getName();
        Utilisateur utilisateur = utilisateurRepository
                .findByEmail(email).orElseThrow();

        // Récupérer les comptes
        List<Compte> comptes = compteService
                .getComptesByEmail(email);

        // Calculer soldes
        BigDecimal soldeTotal = comptes.stream()
                .map(Compte::getSolde)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal soldeCourant = comptes.stream()
                .filter(c -> c.getType() ==
                        Compte.TypeCompte.COURANT)
                .map(Compte::getSolde)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal soldeEpargne = comptes.stream()
                .filter(c -> c.getType() ==
                        Compte.TypeCompte.EPARGNE)
                .map(Compte::getSolde)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // Notifications non lues
        long notificationsNonLues = notificationRepository
                .countByUtilisateurAndLu(utilisateur, false);

        // Dernières transactions
        var transactions = transactionService
                .getDernieresTransactions(email);

        model.addAttribute("utilisateur", utilisateur);
        model.addAttribute("soldeTotal", soldeTotal);
        model.addAttribute("soldeCourant", soldeCourant);
        model.addAttribute("soldeEpargne", soldeEpargne);
        model.addAttribute("notificationsNonLues",
                notificationsNonLues);
        model.addAttribute("transactions", transactions);

        return "client/dashboard";
    }
}