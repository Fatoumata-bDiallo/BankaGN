package com.bankagn.bankagn.controller;

import com.bankagn.bankagn.entity.Carte;
import com.bankagn.bankagn.entity.Compte;
import com.bankagn.bankagn.service.impl.CarteService;
import com.bankagn.bankagn.service.impl.CompteService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Controller
@RequestMapping("/client/cartes")
@RequiredArgsConstructor
public class CarteController {

    private final CarteService carteService;
    private final CompteService compteService;

    @GetMapping
    public String mesCartes(Authentication authentication,
                            Model model) {
        String email = authentication.getName();
        List<Carte> cartes = carteService
                .getCartesByEmail(email);
        List<Compte> comptes = compteService
                .getComptesByEmail(email);
        model.addAttribute("cartes", cartes);
        model.addAttribute("comptes", comptes);
        return "client/cartes";
    }

    @PostMapping("/creer")
    public String creerCarte(
            @RequestParam Long compteId,
            @RequestParam String type,
            RedirectAttributes redirectAttributes) {
        try {
            Carte.TypeCarte typeCarte =
                    Carte.TypeCarte.valueOf(type);
            carteService.creerCarte(compteId, typeCarte);
            redirectAttributes.addFlashAttribute("succes",
                    "Carte " + type + " créée avec succès !");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("erreur",
                    "Erreur lors de la création !");
        }
        return "redirect:/client/cartes";
    }

    @GetMapping("/bloquer/{id}")
    public String bloquerCarte(
            @PathVariable Long id,
            RedirectAttributes redirectAttributes) {
        try {
            carteService.bloquerCarte(id);
            redirectAttributes.addFlashAttribute("succes",
                    "Carte bloquée !");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("erreur",
                    "Erreur !");
        }
        return "redirect:/client/cartes";
    }

    @GetMapping("/debloquer/{id}")
    public String debloquerCarte(
            @PathVariable Long id,
            RedirectAttributes redirectAttributes) {
        try {
            carteService.debloquerCarte(id);
            redirectAttributes.addFlashAttribute("succes",
                    "Carte débloquée !");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("erreur",
                    "Erreur !");
        }
        return "redirect:/client/cartes";
    }

    @GetMapping("/supprimer/{id}")
    public String supprimerCarte(
            @PathVariable Long id,
            RedirectAttributes redirectAttributes) {
        try {
            carteService.supprimerCarte(id);
            redirectAttributes.addFlashAttribute("succes",
                    "Carte supprimée !");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("erreur",
                    "Erreur !");
        }
        return "redirect:/client/cartes";
    }
}