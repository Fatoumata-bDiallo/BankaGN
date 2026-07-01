package com.bankagn.bankagn.controller;

import com.bankagn.bankagn.entity.Compte;
import com.bankagn.bankagn.service.impl.CompteService;
import com.bankagn.bankagn.service.impl.TransactionService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
import java.util.List;

@Controller
@RequestMapping("/client")
@RequiredArgsConstructor
public class TransactionController {

    private final TransactionService transactionService;
    private final CompteService compteService;

    // Page historique transactions
    @GetMapping("/transactions")
    public String transactions(Authentication authentication,
                               Model model) {
        String email = authentication.getName();
        model.addAttribute("transactions",
                transactionService.getTransactionsByEmail(email));
        model.addAttribute("comptes",
                compteService.getComptesByEmail(email));
        return "client/transactions";
    }

    // Page dépôt
    @GetMapping("/depot")
    public String depotPage(Authentication authentication,
                            Model model) {
        String email = authentication.getName();
        List<Compte> comptes = compteService
                .getComptesByEmail(email);
        model.addAttribute("comptes", comptes);
        return "client/depot";
    }

    // Effectuer dépôt
    @PostMapping("/depot")
    public String effectuerDepot(
            @RequestParam Long compteId,
            @RequestParam BigDecimal montant,
            @RequestParam(required = false) String description,
            RedirectAttributes redirectAttributes) {
        try {
            transactionService.effectuerDepot(
                    compteId, montant, description);
            redirectAttributes.addFlashAttribute("succes",
                    "Dépôt de " + montant +
                            " GNF effectué avec succès !");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("erreur",
                    e.getMessage());
        }
        return "redirect:/client/depot";
    }

    // Page retrait
    @GetMapping("/retrait")
    public String retraitPage(Authentication authentication,
                              Model model) {
        String email = authentication.getName();
        List<Compte> comptes = compteService
                .getComptesByEmail(email);
        model.addAttribute("comptes", comptes);
        return "client/retrait";
    }

    // Effectuer retrait
    @PostMapping("/retrait")
    public String effectuerRetrait(
            @RequestParam Long compteId,
            @RequestParam BigDecimal montant,
            @RequestParam(required = false) String description,
            RedirectAttributes redirectAttributes) {
        try {
            transactionService.effectuerRetrait(
                    compteId, montant, description);
            redirectAttributes.addFlashAttribute("succes",
                    "Retrait de " + montant +
                            " GNF effectué avec succès !");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("erreur",
                    e.getMessage());
        }
        return "redirect:/client/retrait";
    }

    // Page transfert
    @GetMapping("/transfert")
    public String transfertPage(Authentication authentication,
                                Model model) {
        String email = authentication.getName();
        List<Compte> comptes = compteService
                .getComptesByEmail(email);
        model.addAttribute("comptes", comptes);
        return "client/transfert";
    }

    // Effectuer transfert
    @PostMapping("/transfert")
    public String effectuerTransfert(
            @RequestParam Long compteSourceId,
            @RequestParam String numeroDestination,
            @RequestParam BigDecimal montant,
            @RequestParam(required = false) String description,
            RedirectAttributes redirectAttributes) {
        try {
            transactionService.effectuerTransfert(
                    compteSourceId, numeroDestination,
                    montant, description);
            redirectAttributes.addFlashAttribute("succes",
                    "Transfert de " + montant +
                            " GNF effectué avec succès !");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("erreur",
                    e.getMessage());
        }
        return "redirect:/client/transfert";
    }
}