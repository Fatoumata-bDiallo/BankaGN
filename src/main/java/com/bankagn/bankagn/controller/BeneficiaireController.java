package com.bankagn.bankagn.controller;

import com.bankagn.bankagn.entity.Beneficiaire;
import com.bankagn.bankagn.entity.Utilisateur;
import com.bankagn.bankagn.repository.BeneficiaireRepository;
import com.bankagn.bankagn.repository.CompteRepository;
import com.bankagn.bankagn.repository.UtilisateurRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Controller
@RequestMapping("/client/beneficiaires")
@RequiredArgsConstructor
public class BeneficiaireController {

    private final BeneficiaireRepository beneficiaireRepository;
    private final UtilisateurRepository utilisateurRepository;
    private final CompteRepository compteRepository;

    @GetMapping
    public String mesBeneficiaires(
            Authentication authentication,
            Model model) {
        String email = authentication.getName();
        Utilisateur utilisateur = utilisateurRepository
                .findByEmail(email).orElseThrow();

        List<Beneficiaire> beneficiaires =
                beneficiaireRepository
                        .findByUtilisateur(utilisateur);

        model.addAttribute("beneficiaires", beneficiaires);
        return "client/beneficiaires";
    }

    @PostMapping("/ajouter")
    public String ajouterBeneficiaire(
            @RequestParam String nom,
            @RequestParam String numeroCompte,
            @RequestParam(required = false) String telephone,
            @RequestParam(required = false) String description,
            Authentication authentication,
            RedirectAttributes redirectAttributes) {
        try {
            String email = authentication.getName();
            Utilisateur utilisateur = utilisateurRepository
                    .findByEmail(email).orElseThrow();

            // Vérifier si le compte existe
            boolean compteExiste = compteRepository
                    .existsByNumeroCompte(numeroCompte);
            if (!compteExiste) {
                redirectAttributes.addFlashAttribute("erreur",
                        "⚠️ Le numéro de compte " +
                                numeroCompte + " n'existe pas !");
                return "redirect:/client/beneficiaires";
            }

            // Vérifier si déjà ajouté
            boolean dejaExiste = beneficiaireRepository
                    .existsByUtilisateurAndNumeroCompte(
                            utilisateur, numeroCompte);
            if (dejaExiste) {
                redirectAttributes.addFlashAttribute("erreur",
                        "⚠️ Ce bénéficiaire est déjà " +
                                "dans votre liste !");
                return "redirect:/client/beneficiaires";
            }

            Beneficiaire beneficiaire = Beneficiaire.builder()
                    .nom(nom)
                    .numeroCompte(numeroCompte)
                    .telephone(telephone)
                    .description(description)
                    .utilisateur(utilisateur)
                    .build();

            beneficiaireRepository.save(beneficiaire);
            redirectAttributes.addFlashAttribute("succes",
                    "✅ Bénéficiaire " + nom +
                            " ajouté avec succès !");

        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("erreur",
                    "⚠️ Erreur : " + e.getMessage());
        }
        return "redirect:/client/beneficiaires";
    }

    @GetMapping("/supprimer/{id}")
    public String supprimerBeneficiaire(
            @PathVariable Long id,
            RedirectAttributes redirectAttributes) {
        try {
            beneficiaireRepository.deleteById(id);
            redirectAttributes.addFlashAttribute("succes",
                    "✅ Bénéficiaire supprimé !");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("erreur",
                    "⚠️ Erreur lors de la suppression !");
        }
        return "redirect:/client/beneficiaires";
    }
}