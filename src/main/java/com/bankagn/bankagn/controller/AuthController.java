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
import com.bankagn.bankagn.service.impl.EmailService;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.Base64;
import java.util.List;
import java.util.Random;
import java.util.UUID;

@Controller
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final UtilisateurRepository utilisateurRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final NotificationRepository notificationRepository;
    private final AlerteFraudeRepository alerteFraudeRepository;
    private final JournalAuditRepository journalAuditRepository;
    private final EmailService emailService;

    private static final String BASE_URL =
            "https://bankagn-production.up.railway.app";

    private final HttpSessionSecurityContextRepository
            securityContextRepository =
            new HttpSessionSecurityContextRepository();

    @GetMapping("/login")
    public String loginPage() {
        return "auth/login";
    }

    @GetMapping("/otp-redirect")
    public String otpRedirect(
            Authentication authentication,
            HttpSession session,
            Model model) {

        String email = authentication.getName();
        Utilisateur utilisateur = utilisateurRepository
                .findByEmail(email).orElse(null);

        if (utilisateur == null) {
            return "redirect:/auth/login";
        }

        String otp = String.format("%06d",
                new Random().nextInt(999999));
        utilisateur.setOtpCode(otp);
        utilisateur.setOtpExpiry(
                LocalDateTime.now().plusMinutes(5));
        utilisateurRepository.save(utilisateur);

        session.setAttribute("otpEmail", email);

        String messageOtp =
                "Bonjour " + utilisateur.getPrenom() + ",\n\n" +
                        "Votre code de vérification BankaGN est :\n\n" +
                        "╔══════════════╗\n" +
                        "║   " + otp + "   ║\n" +
                        "╚══════════════╝\n\n" +
                        "Ce code est valable 5 minutes.\n\n" +
                        "Cordialement,\n" +
                        "L'équipe BankaGN";

        emailService.envoyerEmail(email,
                "🔐 Code de vérification BankaGN - " + otp,
                messageOtp);

        model.addAttribute("email", email);
        return "auth/otp";
    }

    @GetMapping("/otp")
    public String otpPage(HttpSession session, Model model) {
        String otpEmail = (String) session
                .getAttribute("otpEmail");
        if (otpEmail == null) {
            return "redirect:/auth/login";
        }
        model.addAttribute("email", otpEmail);
        return "auth/otp";
    }

    @PostMapping("/otp")
    public String verifierOtp(
            @RequestParam String code,
            HttpSession session,
            HttpServletRequest request,
            HttpServletResponse response,
            Model model) {

        String otpEmail = (String) session
                .getAttribute("otpEmail");

        if (otpEmail == null) {
            return "redirect:/auth/login";
        }

        Utilisateur utilisateur = utilisateurRepository
                .findByEmail(otpEmail).orElse(null);

        if (utilisateur == null) {
            return "redirect:/auth/login";
        }

        if (utilisateur.getOtpExpiry() == null ||
                utilisateur.getOtpExpiry()
                        .isBefore(LocalDateTime.now())) {
            model.addAttribute("erreur",
                    "❌ Code expiré ! Reconnectez-vous.");
            model.addAttribute("email", otpEmail);
            return "auth/otp";
        }

        if (!code.equals(utilisateur.getOtpCode())) {
            model.addAttribute("erreur",
                    "❌ Code incorrect ! Vérifiez votre email.");
            model.addAttribute("email", otpEmail);
            return "auth/otp";
        }

        utilisateur.setOtpCode(null);
        utilisateur.setOtpExpiry(null);
        utilisateurRepository.save(utilisateur);

        session.removeAttribute("otpEmail");

        enregistrerAudit("Connexion réussie avec 2FA",
                utilisateur.getPrenom() + " " +
                        utilisateur.getNom() + " connecté",
                otpEmail, JournalAudit.TypeAction.CONNEXION);

        UsernamePasswordAuthenticationToken authToken =
                new UsernamePasswordAuthenticationToken(
                        utilisateur.getEmail(),
                        null,
                        List.of(new SimpleGrantedAuthority(
                                "ROLE_" + utilisateur.getRole()
                                        .name()))
                );

        SecurityContextHolder.getContext()
                .setAuthentication(authToken);

        securityContextRepository.saveContext(
                SecurityContextHolder.getContext(),
                request, response);

        String token = jwtUtil.generateToken(
                otpEmail, utilisateur.getRole().name());

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
            @RequestParam(value = "typePiece",
                    required = false) String typePiece,
            @RequestParam(value = "pieceIdentite",
                    required = false)
            MultipartFile pieceIdentite,
            Model model) {

        if (utilisateurRepository.existsByEmail(email)) {
            model.addAttribute("erreur",
                    "⚠️ Un compte existe déjà avec cet email !");
            return "auth/register";
        }

        String tokenConfirmation = UUID.randomUUID().toString();

        // Traiter la pièce d'identité
        String pieceBase64 = null;
        if (pieceIdentite != null &&
                !pieceIdentite.isEmpty()) {
            try {
                if (pieceIdentite.getSize() >
                        5 * 1024 * 1024) {
                    model.addAttribute("erreur",
                            "⚠️ La pièce ne doit pas " +
                                    "dépasser 5MB !");
                    return "auth/register";
                }
                String contentType =
                        pieceIdentite.getContentType();
                if (contentType == null) {
                    contentType = "image/jpeg";
                }
                pieceBase64 = contentType +
                        ";base64," +
                        Base64.getEncoder()
                                .encodeToString(
                                        pieceIdentite.getBytes());
            } catch (Exception e) {
                System.err.println("Erreur upload : "
                        + e.getMessage());
            }
        }

        Utilisateur utilisateur = Utilisateur.builder()
                .nom(nom)
                .prenom(prenom)
                .email(email)
                .telephone(telephone)
                .motDePasse(passwordEncoder.encode(motDePasse))
                .role(Utilisateur.Role.CLIENT)
                .statut(Utilisateur.Statut.EN_ATTENTE)
                .resetToken(tokenConfirmation)
                .typePiece(typePiece)
                .pieceIdentite(pieceBase64)
                .build();

        utilisateurRepository.save(utilisateur);

        String lienConfirmation = BASE_URL +
                "/auth/confirmer-email/" + tokenConfirmation;

        String messageEmail =
                "Bonjour " + prenom + " " + nom + ",\n\n" +
                        "Bienvenue sur BankaGN !\n\n" +
                        "Pour confirmer votre email, cliquez ici :\n" +
                        lienConfirmation + "\n\n" +
                        "Cordialement,\n" +
                        "L'équipe BankaGN";

        emailService.envoyerEmail(email,
                "✅ Confirmez votre inscription BankaGN",
                messageEmail);

        Utilisateur admin = utilisateurRepository.findAll()
                .stream()
                .filter(u -> u.getRole() ==
                        Utilisateur.Role.ADMIN)
                .findFirst().orElse(null);

        if (admin != null) {
            Notification notification = Notification.builder()
                    .titre("🆕 Nouvelle inscription !")
                    .message("Un nouveau client " +
                            prenom + " " + nom +
                            " (" + email + ") vient de s'inscrire." +
                            (pieceBase64 != null ?
                                    " Pièce : " + typePiece :
                                    " ⚠️ Sans pièce d'identité"))
                    .type(Notification.TypeNotification.SYSTEME)
                    .lu(false)
                    .utilisateur(admin)
                    .build();
            notificationRepository.save(notification);
        }

        enregistrerAudit("Nouvelle inscription",
                prenom + " " + nom +
                        " (" + email + ") inscrit",
                email, JournalAudit.TypeAction.UTILISATEUR);

        model.addAttribute("succes",
                "✅ Inscription réussie ! Un email de " +
                        "confirmation a été envoyé sur " + email + ".");
        return "auth/login";
    }

    @GetMapping("/confirmer-email/{token}")
    public String confirmerEmail(
            @PathVariable String token,
            Model model) {

        Utilisateur utilisateur = utilisateurRepository
                .findAll().stream()
                .filter(u -> token.equals(u.getResetToken()))
                .findFirst().orElse(null);

        if (utilisateur == null) {
            model.addAttribute("erreur",
                    "❌ Lien de confirmation invalide !");
            return "auth/login";
        }

        utilisateur.setResetToken(null);
        utilisateurRepository.save(utilisateur);

        Utilisateur admin = utilisateurRepository.findAll()
                .stream()
                .filter(u -> u.getRole() ==
                        Utilisateur.Role.ADMIN)
                .findFirst().orElse(null);

        if (admin != null) {
            Notification notification = Notification.builder()
                    .titre("✅ Email confirmé !")
                    .message(utilisateur.getPrenom() + " " +
                            utilisateur.getNom() +
                            " a confirmé son email.")
                    .type(Notification.TypeNotification.SYSTEME)
                    .lu(false)
                    .utilisateur(admin)
                    .build();
            notificationRepository.save(notification);
        }

        model.addAttribute("succes",
                "✅ Email confirmé ! Votre compte est en " +
                        "attente de validation par l'administrateur.");
        return "auth/login";
    }

    @GetMapping("/logout")
    public String logout(HttpServletResponse response,
                         HttpSession session) {
        Cookie cookie = new Cookie("jwt", null);
        cookie.setMaxAge(0);
        cookie.setPath("/");
        response.addCookie(cookie);
        session.invalidate();
        return "redirect:/auth/login";
    }

    private void enregistrerAudit(String action,
                                  String details, String effectuePar,
                                  JournalAudit.TypeAction typeAction) {
        JournalAudit journal = JournalAudit.builder()
                .action(action).details(details)
                .effectuePar(effectuePar)
                .typeAction(typeAction).build();
        journalAuditRepository.save(journal);
    }

    private void creerAlerteFraude(Utilisateur utilisateur,
                                   String typeAlerte, String description,
                                   AlerteFraude.NiveauAlerte niveau) {
        AlerteFraude alerte = AlerteFraude.builder()
                .typeAlerte(typeAlerte)
                .description(description)
                .niveau(niveau)
                .statut(AlerteFraude.StatutAlerteEnum.EN_COURS)
                .resolu(false)
                .utilisateur(utilisateur).build();
        alerteFraudeRepository.save(alerte);

        Utilisateur admin = utilisateurRepository.findAll()
                .stream()
                .filter(u -> u.getRole() ==
                        Utilisateur.Role.ADMIN)
                .findFirst().orElse(null);

        if (admin != null) {
            Notification notification = Notification.builder()
                    .titre("🚨 Alerte Fraude - " + niveau)
                    .message("Alerte : " + typeAlerte +
                            " pour " + utilisateur.getPrenom() +
                            " " + utilisateur.getNom())
                    .type(Notification.TypeNotification.ALERTE)
                    .lu(false)
                    .utilisateur(admin).build();
            notificationRepository.save(notification);
        }
    }
}