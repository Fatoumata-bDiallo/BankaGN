package com.bankagn.bankagn.controller;

import com.bankagn.bankagn.entity.AlerteFraude;
import com.bankagn.bankagn.entity.JournalAudit;
import com.bankagn.bankagn.entity.Notification;
import com.bankagn.bankagn.entity.Utilisateur;
import com.bankagn.bankagn.repository.AlerteFraudeRepository;
import com.bankagn.bankagn.repository.JournalAuditRepository;
import com.bankagn.bankagn.repository.NotificationRepository;
import com.bankagn.bankagn.repository.UtilisateurRepository;
import com.bankagn.bankagn.security.JwtUtil;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final UtilisateurRepository utilisateurRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final NotificationRepository notificationRepository;
    private final AlerteFraudeRepository alerteFraudeRepository;
    private final JournalAuditRepository journalAuditRepository;

    @GetMapping("/login")
    public String loginPage() {
        return "auth/login";
    }

    @PostMapping("/login")
    public String login(
            @RequestParam String email,
            @RequestParam String motDePasse,
            HttpServletResponse response,
            Model model) {
        try {
            Utilisateur utilisateur = utilisateurRepository
                    .findByEmail(email).orElse(null);

            if (utilisateur == null) {
                model.addAttribute("erreur",
                        "❌ Aucun compte trouvé avec cet email.");
                return "auth/login";
            }

            // Vérifier statut EN_ATTENTE
            if (utilisateur.getStatut() ==
                    Utilisateur.Statut.EN_ATTENTE) {
                model.addAttribute("erreur",
                        "⏳ Votre compte est en cours de vérification. " +
                                "Vous ne pouvez pas vous connecter tant que " +
                                "l'administrateur n'a pas validé votre inscription. " +
                                "Vous serez notifié dès l'activation de votre compte.");
                return "auth/login";
            }

            // Vérifier statut BLOQUE
            if (utilisateur.getStatut() ==
                    Utilisateur.Statut.BLOQUE) {
                model.addAttribute("erreur",
                        "🔒 Votre compte a été suspendu. " +
                                "Veuillez contacter l'administration BankaGN " +
                                "pour plus d'informations.");
                return "auth/login";
            }

            // Vérifier statut INACTIF
            if (utilisateur.getStatut() ==
                    Utilisateur.Statut.INACTIF) {
                model.addAttribute("erreur",
                        "❌ Votre demande d'inscription a été refusée. " +
                                "Veuillez contacter l'administration BankaGN " +
                                "pour plus d'informations.");
                return "auth/login";
            }

            // Authentifier
            try {
                authenticationManager.authenticate(
                        new UsernamePasswordAuthenticationToken(
                                email, motDePasse));
            } catch (DisabledException e) {
                model.addAttribute("erreur",
                        "⏳ Votre compte est en cours de vérification. " +
                                "Vous ne pouvez pas vous connecter tant que " +
                                "l'administrateur n'a pas validé votre inscription.");
                return "auth/login";
            } catch (LockedException e) {
                model.addAttribute("erreur",
                        "🔒 Votre compte a été suspendu suite à " +
                                "plusieurs tentatives de connexion échouées. " +
                                "Contactez l'administration BankaGN.");
                return "auth/login";
            } catch (Exception e) {
                // Incrémenter tentatives échouées
                utilisateur.setTentativesConnexion(
                        utilisateur.getTentativesConnexion() + 1);
                utilisateurRepository.save(utilisateur);

                // Audit tentative échouée
                enregistrerAudit(
                        "Tentative connexion échouée",
                        "Tentative " +
                                utilisateur.getTentativesConnexion() +
                                "/5 pour " + email,
                        email,
                        JournalAudit.TypeAction.CONNEXION);

                // Alerte après 3 tentatives
                if (utilisateur.getTentativesConnexion() >= 3) {
                    creerAlerteFraude(utilisateur,
                            "Tentatives connexion suspectes",
                            utilisateur.getTentativesConnexion() +
                                    " tentatives échouées pour " + email,
                            AlerteFraude.NiveauAlerte.ELEVE);
                }

                // Blocage après 5 tentatives
                if (utilisateur.getTentativesConnexion() >= 5) {
                    utilisateur.setStatut(
                            Utilisateur.Statut.BLOQUE);
                    utilisateurRepository.save(utilisateur);

                    enregistrerAudit(
                            "Compte bloqué automatiquement",
                            "Compte " + email +
                                    " bloqué après 5 tentatives",
                            "SYSTEME",
                            JournalAudit.TypeAction.UTILISATEUR);

                    model.addAttribute("erreur",
                            "🔒 Votre compte a été suspendu " +
                                    "automatiquement après 5 tentatives " +
                                    "de connexion incorrectes. " +
                                    "Veuillez contacter l'administration BankaGN.");
                    return "auth/login";
                }

                int restantes = 5 -
                        utilisateur.getTentativesConnexion();
                model.addAttribute("erreur",
                        "❌ Email ou mot de passe incorrect. " +
                                "Il vous reste " + restantes +
                                " tentative(s) avant la suspension " +
                                "de votre compte.");
                return "auth/login";
            }

            // Connexion réussie
            utilisateur.setTentativesConnexion(0);
            utilisateurRepository.save(utilisateur);

            enregistrerAudit(
                    "Connexion réussie",
                    utilisateur.getPrenom() + " " +
                            utilisateur.getNom() +
                            " connecté en tant que " +
                            utilisateur.getRole(),
                    email,
                    JournalAudit.TypeAction.CONNEXION);

            String token = jwtUtil.generateToken(
                    email, utilisateur.getRole().name());

            Cookie cookie = new Cookie("jwt", token);
            cookie.setHttpOnly(true);
            cookie.setMaxAge(86400);
            cookie.setPath("/");
            response.addCookie(cookie);

            if (utilisateur.getRole() ==
                    Utilisateur.Role.ADMIN) {
                return "redirect:/admin/dashboard";
            }
            return "redirect:/client/dashboard";

        } catch (Exception e) {
            model.addAttribute("erreur",
                    "❌ Une erreur est survenue. " +
                            "Veuillez réessayer.");
            return "auth/login";
        }
    }

    @GetMapping("/register")
    public String registerPage(Model model) {
        model.addAttribute("utilisateur", new Utilisateur());
        return "auth/register";
    }

    @PostMapping("/register")
    public String register(
            @RequestParam String nom,
            @RequestParam String prenom,
            @RequestParam String email,
            @RequestParam String telephone,
            @RequestParam String motDePasse,
            Model model) {

        if (utilisateurRepository.existsByEmail(email)) {
            model.addAttribute("erreur",
                    "⚠️ Un compte existe déjà avec cet email !");
            return "auth/register";
        }

        Utilisateur utilisateur = Utilisateur.builder()
                .nom(nom)
                .prenom(prenom)
                .email(email)
                .telephone(telephone)
                .motDePasse(passwordEncoder.encode(motDePasse))
                .role(Utilisateur.Role.CLIENT)
                .statut(Utilisateur.Statut.EN_ATTENTE)
                .build();

        utilisateurRepository.save(utilisateur);

        enregistrerAudit(
                "Nouvelle inscription",
                prenom + " " + nom +
                        " (" + email + ") inscrit",
                email,
                JournalAudit.TypeAction.UTILISATEUR);

        // Notifier l'admin
        Utilisateur admin = utilisateurRepository
                .findAll().stream()
                .filter(u -> u.getRole() ==
                        Utilisateur.Role.ADMIN)
                .findFirst().orElse(null);

        if (admin != null) {
            Notification notification = Notification.builder()
                    .titre("🆕 Nouvelle inscription !")
                    .message("Un nouveau client " +
                            prenom + " " + nom +
                            " (" + email + ") " +
                            "vient de s'inscrire. " +
                            "Veuillez valider son compte.")
                    .type(Notification.TypeNotification.SYSTEME)
                    .lu(false)
                    .utilisateur(admin)
                    .build();
            notificationRepository.save(notification);
        }

        model.addAttribute("succes",
                "✅ Votre inscription a bien été enregistrée ! " +
                        "Votre compte est en cours de vérification par " +
                        "notre équipe. Vous recevrez une confirmation " +
                        "dès que votre compte sera activé. " +
                        "Merci de votre confiance en BankaGN !");
        return "auth/login";
    }

    @GetMapping("/logout")
    public String logout(HttpServletResponse response) {
        Cookie cookie = new Cookie("jwt", null);
        cookie.setMaxAge(0);
        cookie.setPath("/");
        response.addCookie(cookie);
        return "redirect:/auth/login";
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

    private void creerAlerteFraude(
            Utilisateur utilisateur,
            String typeAlerte,
            String description,
            AlerteFraude.NiveauAlerte niveau) {

        AlerteFraude alerte = AlerteFraude.builder()
                .typeAlerte(typeAlerte)
                .description(description)
                .niveau(niveau)
                .statut(AlerteFraude.StatutAlerteEnum.EN_COURS)
                .resolu(false)
                .utilisateur(utilisateur)
                .build();
        alerteFraudeRepository.save(alerte);

        Utilisateur admin = utilisateurRepository
                .findAll().stream()
                .filter(u -> u.getRole() ==
                        Utilisateur.Role.ADMIN)
                .findFirst().orElse(null);

        if (admin != null) {
            Notification notification = Notification.builder()
                    .titre("🚨 Alerte Fraude - " + niveau)
                    .message("Alerte : " + typeAlerte +
                            " pour " + utilisateur.getPrenom() +
                            " " + utilisateur.getNom() +
                            ". " + description)
                    .type(Notification.TypeNotification.ALERTE)
                    .lu(false)
                    .utilisateur(admin)
                    .build();
            notificationRepository.save(notification);
        }
    }
}