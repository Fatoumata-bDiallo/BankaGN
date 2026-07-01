package com.bankagn.bankagn.controller;

import com.bankagn.bankagn.entity.*;
import com.bankagn.bankagn.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Controller
@RequestMapping("/admin/rapports")
@RequiredArgsConstructor
public class RapportController {

    private final UtilisateurRepository utilisateurRepository;
    private final CompteRepository compteRepository;
    private final TransactionRepository transactionRepository;
    private final PretRepository pretRepository;
    private final AlerteFraudeRepository alerteFraudeRepository;

    @GetMapping
    public String rapports(
            @RequestParam(required = false,
                    defaultValue = "mois") String periode,
            Model model) {

        LocalDateTime debut = getDebut(periode);
        LocalDateTime fin = LocalDateTime.now();

        // Stats clients
        long totalClients = utilisateurRepository
                .countByRole(Utilisateur.Role.CLIENT);
        long clientsActifs = utilisateurRepository
                .findAll().stream()
                .filter(u -> u.getRole() ==
                        Utilisateur.Role.CLIENT &&
                        u.getStatut() ==
                                Utilisateur.Statut.ACTIF)
                .count();
        long clientsEnAttente = utilisateurRepository
                .findAll().stream()
                .filter(u -> u.getRole() ==
                        Utilisateur.Role.CLIENT &&
                        u.getStatut() ==
                                Utilisateur.Statut.EN_ATTENTE)
                .count();

        // Stats comptes
        long totalComptes = compteRepository.count();
        long comptesCourants = compteRepository.findAll()
                .stream()
                .filter(c -> c.getType() ==
                        Compte.TypeCompte.COURANT)
                .count();
        long comptesEpargne = compteRepository.findAll()
                .stream()
                .filter(c -> c.getType() ==
                        Compte.TypeCompte.EPARGNE)
                .count();

        // Stats transactions
        List<Transaction> transactions =
                transactionRepository.findAll().stream()
                        .filter(t -> t.getDateTransaction() != null
                                && t.getDateTransaction().isAfter(debut)
                                && t.getDateTransaction().isBefore(fin))
                        .toList();

        long totalTransactions = transactions.size();

        BigDecimal totalDepots = transactions.stream()
                .filter(t -> t.getType() ==
                        Transaction.TypeTransaction.DEPOT)
                .map(Transaction::getMontant)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalRetraits = transactions.stream()
                .filter(t -> t.getType() ==
                        Transaction.TypeTransaction.RETRAIT)
                .map(Transaction::getMontant)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalTransferts = transactions.stream()
                .filter(t -> t.getType() ==
                        Transaction.TypeTransaction.TRANSFERT)
                .map(Transaction::getMontant)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // Stats prêts
        List<Pret> prets = pretRepository.findAll().stream()
                .filter(p -> p.getDateCreation() != null
                        && p.getDateCreation().isAfter(debut)
                        && p.getDateCreation().isBefore(fin))
                .toList();

        long totalPrets = prets.size();
        long pretsAcceptes = prets.stream()
                .filter(p -> p.getStatut() ==
                        Pret.StatutPret.ACCEPTE)
                .count();
        long pretsRefuses = prets.stream()
                .filter(p -> p.getStatut() ==
                        Pret.StatutPret.REFUSE)
                .count();
        long pretsEnAttente = prets.stream()
                .filter(p -> p.getStatut() ==
                        Pret.StatutPret.EN_ATTENTE)
                .count();

        BigDecimal montantPrets = prets.stream()
                .filter(p -> p.getStatut() ==
                        Pret.StatutPret.ACCEPTE)
                .map(Pret::getMontant)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // Stats fraudes
        long totalFraudes = alerteFraudeRepository
                .countByStatut(
                        AlerteFraude.StatutAlerteEnum.EN_COURS);
        long fraudesResolues = alerteFraudeRepository
                .countByStatut(
                        AlerteFraude.StatutAlerteEnum.RESOLUE);

        model.addAttribute("periode", periode);
        model.addAttribute("debut", debut);
        model.addAttribute("fin", fin);
        model.addAttribute("totalClients", totalClients);
        model.addAttribute("clientsActifs", clientsActifs);
        model.addAttribute("clientsEnAttente",
                clientsEnAttente);
        model.addAttribute("totalComptes", totalComptes);
        model.addAttribute("comptesCourants", comptesCourants);
        model.addAttribute("comptesEpargne", comptesEpargne);
        model.addAttribute("totalTransactions",
                totalTransactions);
        model.addAttribute("totalDepots", totalDepots);
        model.addAttribute("totalRetraits", totalRetraits);
        model.addAttribute("totalTransferts", totalTransferts);
        model.addAttribute("totalPrets", totalPrets);
        model.addAttribute("pretsAcceptes", pretsAcceptes);
        model.addAttribute("pretsRefuses", pretsRefuses);
        model.addAttribute("pretsEnAttente", pretsEnAttente);
        model.addAttribute("montantPrets", montantPrets);
        model.addAttribute("totalFraudes", totalFraudes);
        model.addAttribute("fraudesResolues", fraudesResolues);

        return "admin/rapports";
    }

    private LocalDateTime getDebut(String periode) {
        return switch (periode) {
            case "jour" -> LocalDateTime.now().minusDays(1);
            case "semaine" -> LocalDateTime.now().minusWeeks(1);
            case "mois" -> LocalDateTime.now().minusMonths(1);
            case "annee" -> LocalDateTime.now().minusYears(1);
            default -> LocalDateTime.now().minusMonths(1);
        };
    }
}