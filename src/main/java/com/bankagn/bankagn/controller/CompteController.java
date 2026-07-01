package com.bankagn.bankagn.controller;

import com.bankagn.bankagn.entity.Compte;
import com.bankagn.bankagn.entity.JournalAudit;
import com.bankagn.bankagn.repository.JournalAuditRepository;
import com.bankagn.bankagn.service.impl.CompteService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Controller
@RequestMapping("/client/comptes")
@RequiredArgsConstructor
public class CompteController {

    private final CompteService compteService;
    private final JournalAuditRepository journalAuditRepository;

    @GetMapping
    public String mesComptes(Authentication authentication,
                             Model model) {
        String email = authentication.getName();
        List<Compte> comptes = compteService
                .getComptesByEmail(email);
        model.addAttribute("comptes", comptes);
        return "client/comptes";
    }

    @GetMapping("/nouveau")
    public String nouveauCompte() {
        return "client/nouveau-compte";
    }

    @PostMapping("/creer")
    public String creerCompte(
            @RequestParam String type,
            Authentication authentication,
            RedirectAttributes redirectAttributes) {
        try {
            String email = authentication.getName();
            Compte compte = compteService
                    .creerCompte(email, type);

            // Audit
            enregistrerAudit(
                    "Création compte bancaire",
                    "Compte " + type + " créé : " +
                            compte.getNumeroCompte(),
                    email,
                    JournalAudit.TypeAction.COMPTE);

            redirectAttributes.addFlashAttribute("succes",
                    "✅ Compte " + type +
                            " créé avec succès ! Numéro : " +
                            compte.getNumeroCompte());
        } catch (RuntimeException e) {
            redirectAttributes.addFlashAttribute("erreur",
                    "⚠️ " + e.getMessage());
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("erreur",
                    "⚠️ Une erreur est survenue. " +
                            "Veuillez réessayer.");
        }
        return "redirect:/client/comptes";
    }

    @GetMapping("/fermer/{id}")
    public String fermerCompte(
            @PathVariable Long id,
            Authentication authentication,
            RedirectAttributes redirectAttributes) {
        try {
            compteService.fermerCompte(id);

            // Audit
            enregistrerAudit(
                    "Fermeture compte bancaire",
                    "Compte ID " + id + " fermé",
                    authentication.getName(),
                    JournalAudit.TypeAction.COMPTE);

            redirectAttributes.addFlashAttribute("succes",
                    "✅ Compte fermé avec succès !");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("erreur",
                    "⚠️ Erreur lors de la fermeture !");
        }
        return "redirect:/client/comptes";
    }

    private void enregistrerAudit(
            String action,
            String details,
            String effectuePar,
            JournalAudit.TypeAction typeAction) {
        JournalAudit journal = JournalAudit.builder()
                .action(action)
                .details(details)
                .effectuePar(effectuePar)
                .typeAction(typeAction)
                .build();
        journalAuditRepository.save(journal);
    }
}