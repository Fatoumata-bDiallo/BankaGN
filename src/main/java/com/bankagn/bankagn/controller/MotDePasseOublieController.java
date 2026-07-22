package com.bankagn.bankagn.controller;

import com.bankagn.bankagn.entity.Utilisateur;
import com.bankagn.bankagn.repository.UtilisateurRepository;
import com.bankagn.bankagn.service.impl.EmailService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDateTime;
import java.util.UUID;

@Controller
@RequiredArgsConstructor
public class MotDePasseOublieController {

    private final UtilisateurRepository utilisateurRepository;
    private final EmailService emailService;
    private final PasswordEncoder passwordEncoder;

    private static final String BASE_URL =
            "https://bankagn-production.up.railway.app";

    @GetMapping("/auth/mot-de-passe-oublie")
    public String motDePasseOubliePage() {
        return "auth/mot-de-passe-oublie";
    }

    @PostMapping("/auth/mot-de-passe-oublie")
    public String envoyerLien(
            @RequestParam String email,
            Model model) {

        Utilisateur utilisateur = utilisateurRepository
                .findByEmail(email).orElse(null);

        if (utilisateur == null) {
            model.addAttribute("erreur",
                    "❌ Aucun compte trouvé avec cet email !");
            return "auth/mot-de-passe-oublie";
        }

        if (utilisateur.getStatut() !=
                Utilisateur.Statut.ACTIF) {
            model.addAttribute("erreur",
                    "❌ Ce compte n'est pas actif !");
            return "auth/mot-de-passe-oublie";
        }

        // Générer token unique
        String token = UUID.randomUUID().toString();
        utilisateur.setResetToken(token);
        utilisateur.setResetTokenExpiry(
                LocalDateTime.now().plusMinutes(30));
        utilisateurRepository.save(utilisateur);

        // Lien Railway
        String lien = BASE_URL + "/auth/reinitialiser/" + token;

        String message =
                "Bonjour " + utilisateur.getPrenom() + ",\n\n" +
                        "Vous avez demandé la réinitialisation de " +
                        "votre mot de passe BankaGN.\n\n" +
                        "Cliquez sur ce lien pour créer " +
                        "un nouveau mot de passe :\n" +
                        lien + "\n\n" +
                        "Ce lien est valable 30 minutes.\n\n" +
                        "Si vous n'êtes pas à l'origine de cette " +
                        "demande, ignorez cet email.\n\n" +
                        "Cordialement,\n" +
                        "L'équipe BankaGN\n" +
                        "contact@bankagn.com | +224 626 335 841";

        emailService.envoyerEmail(email,
                "🔐 Réinitialisation de votre mot de passe BankaGN",
                message);

        model.addAttribute("succes",
                "✅ Un lien de réinitialisation a été envoyé " +
                        "sur " + email + ". Vérifiez votre boîte mail ! " +
                        "(Valable 30 minutes)");
        return "auth/mot-de-passe-oublie";
    }

    @GetMapping("/auth/reinitialiser/{token}")
    public String reinitialiserPage(
            @PathVariable String token,
            Model model) {

        Utilisateur utilisateur = utilisateurRepository
                .findAll().stream()
                .filter(u -> token.equals(u.getResetToken()))
                .findFirst().orElse(null);

        if (utilisateur == null ||
                utilisateur.getResetTokenExpiry() == null ||
                utilisateur.getResetTokenExpiry()
                        .isBefore(LocalDateTime.now())) {
            model.addAttribute("erreur",
                    "❌ Ce lien est invalide ou expiré !");
            return "auth/reinitialiser";
        }

        model.addAttribute("token", token);
        return "auth/reinitialiser";
    }

    @PostMapping("/auth/reinitialiser")
    public String reinitialiser(
            @RequestParam String token,
            @RequestParam String nouveauMotDePasse,
            @RequestParam String confirmerMotDePasse,
            RedirectAttributes redirectAttributes) {

        Utilisateur utilisateur = utilisateurRepository
                .findAll().stream()
                .filter(u -> token.equals(u.getResetToken()))
                .findFirst().orElse(null);

        if (utilisateur == null ||
                utilisateur.getResetTokenExpiry()
                        .isBefore(LocalDateTime.now())) {
            redirectAttributes.addFlashAttribute("erreur",
                    "❌ Ce lien est invalide ou expiré !");
            return "redirect:/auth/login";
        }

        if (!nouveauMotDePasse.equals(confirmerMotDePasse)) {
            redirectAttributes.addFlashAttribute("erreur",
                    "❌ Les mots de passe ne correspondent pas !");
            return "redirect:/auth/reinitialiser/" + token;
        }

        if (nouveauMotDePasse.length() < 6) {
            redirectAttributes.addFlashAttribute("erreur",
                    "❌ Le mot de passe doit contenir " +
                            "au moins 6 caractères !");
            return "redirect:/auth/reinitialiser/" + token;
        }

        utilisateur.setMotDePasse(
                passwordEncoder.encode(nouveauMotDePasse));
        utilisateur.setResetToken(null);
        utilisateur.setResetTokenExpiry(null);
        utilisateurRepository.save(utilisateur);

        redirectAttributes.addFlashAttribute("succes",
                "✅ Mot de passe réinitialisé avec succès ! " +
                        "Connectez-vous avec votre nouveau mot de passe.");
        return "redirect:/auth/login";
    }
}