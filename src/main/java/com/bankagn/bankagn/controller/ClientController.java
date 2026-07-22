package com.bankagn.bankagn.controller;

import com.bankagn.bankagn.entity.Compte;
import com.bankagn.bankagn.entity.Notification;
import com.bankagn.bankagn.entity.Utilisateur;
import com.bankagn.bankagn.repository.NotificationRepository;
import com.bankagn.bankagn.repository.UtilisateurRepository;
import com.bankagn.bankagn.service.impl.CompteService;
import com.bankagn.bankagn.service.impl.TransactionService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

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
    private final PasswordEncoder passwordEncoder;

    @GetMapping("/dashboard")
    public String dashboard(Authentication authentication,
                            Model model) {

        String email = authentication.getName();
        Utilisateur utilisateur = utilisateurRepository
                .findByEmail(email).orElseThrow();

        List<Compte> comptes = compteService
                .getComptesByEmail(email);

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

        long notificationsNonLues = notificationRepository
                .countByUtilisateurAndLu(utilisateur, false);

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

    // Page profil
    @GetMapping("/profil")
    public String profil(Authentication authentication,
                         Model model) {
        String email = authentication.getName();
        Utilisateur utilisateur = utilisateurRepository
                .findByEmail(email).orElseThrow();
        model.addAttribute("utilisateur", utilisateur);
        return "client/profil";
    }

    // Modifier mot de passe
    @PostMapping("/profil/modifier-mot-de-passe")
    public String modifierMotDePasse(
            @RequestParam String ancienMotDePasse,
            @RequestParam String nouveauMotDePasse,
            @RequestParam String confirmerMotDePasse,
            Authentication authentication,
            RedirectAttributes redirectAttributes) {

        String email = authentication.getName();
        Utilisateur utilisateur = utilisateurRepository
                .findByEmail(email).orElseThrow();

        // Vérifier ancien mot de passe
        if (!passwordEncoder.matches(
                ancienMotDePasse,
                utilisateur.getMotDePasse())) {
            redirectAttributes.addFlashAttribute("erreur",
                    "❌ Votre ancien mot de passe est incorrect !");
            return "redirect:/client/profil";
        }

        // Vérifier que les nouveaux correspondent
        if (!nouveauMotDePasse.equals(confirmerMotDePasse)) {
            redirectAttributes.addFlashAttribute("erreur",
                    "❌ Les nouveaux mots de passe ne correspondent pas !");
            return "redirect:/client/profil";
        }

        // Vérifier longueur minimum
        if (nouveauMotDePasse.length() < 6) {
            redirectAttributes.addFlashAttribute("erreur",
                    "❌ Le mot de passe doit contenir au moins 6 caractères !");
            return "redirect:/client/profil";
        }

        // Vérifier que nouveau ≠ ancien
        if (passwordEncoder.matches(
                nouveauMotDePasse,
                utilisateur.getMotDePasse())) {
            redirectAttributes.addFlashAttribute("erreur",
                    "❌ Le nouveau mot de passe doit être différent de l'ancien !");
            return "redirect:/client/profil";
        }

        // Mettre à jour
        utilisateur.setMotDePasse(
                passwordEncoder.encode(nouveauMotDePasse));
        utilisateurRepository.save(utilisateur);

        // Notification
        Notification notification = Notification.builder()
                .titre("🔐 Mot de passe modifié")
                .message("Votre mot de passe a été modifié " +
                        "avec succès. Si vous n'êtes pas à " +
                        "l'origine de cette modification, " +
                        "contactez l'administration.")
                .type(Notification.TypeNotification.SYSTEME)
                .lu(false)
                .utilisateur(utilisateur)
                .build();
        notificationRepository.save(notification);

        redirectAttributes.addFlashAttribute("succes",
                "✅ Mot de passe modifié avec succès !");
        return "redirect:/client/profil";
    }
}