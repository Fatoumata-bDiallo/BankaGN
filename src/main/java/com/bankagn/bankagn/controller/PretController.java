package com.bankagn.bankagn.controller;

import com.bankagn.bankagn.entity.Compte;
import com.bankagn.bankagn.entity.Pret;
import com.bankagn.bankagn.service.impl.CompteService;
import com.bankagn.bankagn.service.impl.PretService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
import java.util.List;

@Controller
@RequiredArgsConstructor
public class PretController {

    private final PretService pretService;
    private final CompteService compteService;

    // Page prêts client
    @GetMapping("/client/prets")
    public String messPrets(Authentication authentication,
                            Model model) {
        String email = authentication.getName();
        List<Pret> prets = pretService.getPretsByEmail(email);
        List<Compte> comptes = compteService
                .getComptesByEmail(email);
        model.addAttribute("prets", prets);
        model.addAttribute("comptes", comptes);
        return "client/prets";
    }

    // Demander un prêt
    @PostMapping("/client/prets/demander")
    public String demanderPret(
            @RequestParam BigDecimal montant,
            @RequestParam Integer dureeMois,
            @RequestParam BigDecimal tauxInteret,
            @RequestParam(required = false) String motif,
            @RequestParam(required = false) Long compteId,
            Authentication authentication,
            RedirectAttributes redirectAttributes) {
        try {
            String email = authentication.getName();
            pretService.demanderPret(email, montant,
                    dureeMois, tauxInteret, motif, compteId);
            redirectAttributes.addFlashAttribute("succes",
                    "Demande de prêt envoyée avec succès !");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("erreur",
                    e.getMessage());
        }
        return "redirect:/client/prets";
    }

    // Admin - tous les prêts
    @GetMapping("/admin/prets")
    public String adminPrets(Model model) {
        model.addAttribute("prets", pretService.getAllPrets());
        return "admin/prets";
    }

    // Admin - accepter
    @GetMapping("/admin/prets/accepter/{id}")
    public String accepterPret(@PathVariable Long id,
                               RedirectAttributes redirectAttributes) {
        try {
            pretService.accepterPret(id);
            redirectAttributes.addFlashAttribute("succes",
                    "Prêt accepté !");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("erreur",
                    e.getMessage());
        }
        return "redirect:/admin/prets";
    }

    // Admin - refuser
    @GetMapping("/admin/prets/refuser/{id}")
    public String refuserPret(@PathVariable Long id,
                              RedirectAttributes redirectAttributes) {
        try {
            pretService.refuserPret(id);
            redirectAttributes.addFlashAttribute("succes",
                    "Prêt refusé !");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("erreur",
                    e.getMessage());
        }
        return "redirect:/admin/prets";
    }
}