package com.bankagn.bankagn.controller;

import com.bankagn.bankagn.dto.ApiResponse;
import com.bankagn.bankagn.dto.AuthResponse;
import com.bankagn.bankagn.dto.LoginRequest;
import com.bankagn.bankagn.dto.OtpVerifyRequest;
import com.bankagn.bankagn.entity.JournalAudit;
import com.bankagn.bankagn.entity.Notification;
import com.bankagn.bankagn.entity.Utilisateur;
import com.bankagn.bankagn.repository.JournalAuditRepository;
import com.bankagn.bankagn.repository.NotificationRepository;
import com.bankagn.bankagn.repository.UtilisateurRepository;
import com.bankagn.bankagn.security.JwtUtil;
import com.bankagn.bankagn.service.impl.EmailService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.Base64;
import java.util.Random;
import java.util.UUID;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthApiController {

    private final UtilisateurRepository utilisateurRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final NotificationRepository notificationRepository;
    private final JournalAuditRepository journalAuditRepository;
    private final EmailService emailService;

    // ---------- INSCRIPTION AVEC KYC ----------
    @PostMapping(value = "/register", consumes = "multipart/form-data")
    public ResponseEntity<ApiResponse<Void>> register(
            @RequestParam String nom,
            @RequestParam String prenom,
            @RequestParam String email,
            @RequestParam String telephone,
            @RequestParam String motDePasse,
            @RequestParam(value = "typePiece", required = false) String typePiece,
            @RequestParam(value = "pieceIdentite", required = false) MultipartFile pieceIdentite) {

        if (utilisateurRepository.existsByEmail(email)) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(ApiResponse.error("Un compte existe déjà avec cet email."));
        }

        String pieceBase64 = null;
        if (pieceIdentite != null && !pieceIdentite.isEmpty()) {
            if (pieceIdentite.getSize() > 5 * 1024 * 1024) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(ApiResponse.error("La pièce d'identité ne doit pas dépasser 5 Mo."));
            }
            try {
                String contentType = pieceIdentite.getContentType();
                if (contentType == null) contentType = "image/jpeg";
                pieceBase64 = contentType + ";base64,"
                        + Base64.getEncoder().encodeToString(pieceIdentite.getBytes());
            } catch (Exception e) {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body(ApiResponse.error("Erreur lors du traitement de la pièce d'identité."));
            }
        }

        String tokenConfirmation = UUID.randomUUID().toString();

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

        String lienConfirmation = "https://bankagn-production.up.railway.app"
                + "/auth/confirmer-email/" + tokenConfirmation;

        emailService.envoyerEmail(email,
                "✅ Confirmez votre inscription BankaGN",
                "Bonjour " + prenom + " " + nom + ",\n\n"
                        + "Bienvenue sur BankaGN !\n\n"
                        + "Pour confirmer votre email, cliquez ici :\n"
                        + lienConfirmation + "\n\n"
                        + "Cordialement,\nL'équipe BankaGN");

        notifierAdmin("🆕 Nouvelle inscription !",
                "Un nouveau client " + prenom + " " + nom + " (" + email
                        + ") vient de s'inscrire."
                        + (pieceBase64 != null ? " Pièce : " + typePiece : " ⚠️ Sans pièce d'identité"));

        enregistrerAudit("Nouvelle inscription (API)",
                prenom + " " + nom + " (" + email + ") inscrit",
                email, JournalAudit.TypeAction.UTILISATEUR);

        return ResponseEntity.ok(ApiResponse.ok(
                "Inscription réussie. Un email de confirmation a été envoyé à " + email + ".",
                null));
    }

    // ---------- ÉTAPE 1 : LOGIN (email + mot de passe) ----------
    @PostMapping("/login")
    public ResponseEntity<ApiResponse<String>> login(@RequestBody LoginRequest request) {

        Utilisateur utilisateur = utilisateurRepository
                .findByEmail(request.getEmail()).orElse(null);

        if (utilisateur == null
                || !passwordEncoder.matches(request.getMotDePasse(), utilisateur.getMotDePasse())) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.error("Email ou mot de passe incorrect."));
        }

        if (utilisateur.getStatut() == Utilisateur.Statut.EN_ATTENTE) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(ApiResponse.error("Compte en attente de validation par l'administrateur."));
        }
        if (utilisateur.getStatut() == Utilisateur.Statut.BLOQUE) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(ApiResponse.error("Compte bloqué. Contactez l'administrateur."));
        }
        if (utilisateur.getStatut() == Utilisateur.Statut.INACTIF) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(ApiResponse.error("Compte inactif."));
        }

        envoyerOtp(utilisateur);

        return ResponseEntity.ok(ApiResponse.ok(
                "Code de vérification envoyé à " + utilisateur.getEmail(),
                utilisateur.getEmail()));
    }

    // ---------- RENVOYER LE CODE OTP ----------
    @PostMapping("/resend-otp")
    public ResponseEntity<ApiResponse<Void>> resendOtp(@RequestBody LoginRequest request) {
        Utilisateur utilisateur = utilisateurRepository
                .findByEmail(request.getEmail()).orElse(null);

        if (utilisateur == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.error("Utilisateur introuvable."));
        }

        envoyerOtp(utilisateur);
        return ResponseEntity.ok(ApiResponse.ok("Nouveau code envoyé.", null));
    }

    // ---------- ÉTAPE 2 : VÉRIFICATION OTP -> JWT ----------
    @PostMapping("/verify-otp")
    public ResponseEntity<ApiResponse<AuthResponse>> verifyOtp(@RequestBody OtpVerifyRequest request) {

        Utilisateur utilisateur = utilisateurRepository
                .findByEmail(request.getEmail()).orElse(null);

        if (utilisateur == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.error("Utilisateur introuvable."));
        }

        if (utilisateur.getOtpExpiry() == null
                || utilisateur.getOtpExpiry().isBefore(LocalDateTime.now())) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ApiResponse.error("Code expiré. Reconnectez-vous ou demandez un nouveau code."));
        }

        if (!request.getCode().equals(utilisateur.getOtpCode())) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ApiResponse.error("Code incorrect."));
        }

        utilisateur.setOtpCode(null);
        utilisateur.setOtpExpiry(null);
        utilisateur.setDerniereConnexion(LocalDateTime.now());
        utilisateurRepository.save(utilisateur);

        enregistrerAudit("Connexion réussie avec 2FA (API)",
                utilisateur.getPrenom() + " " + utilisateur.getNom() + " connecté",
                utilisateur.getEmail(), JournalAudit.TypeAction.CONNEXION);

        String token = jwtUtil.generateToken(
                utilisateur.getEmail(), utilisateur.getRole().name());

        AuthResponse authResponse = new AuthResponse(
                token,
                utilisateur.getId(),
                utilisateur.getNom(),
                utilisateur.getPrenom(),
                utilisateur.getEmail(),
                utilisateur.getRole().name());

        return ResponseEntity.ok(ApiResponse.ok("Connexion réussie.", authResponse));
    }

    // ---------- PROFIL COURANT (pour restaurer la session côté React) ----------
    @GetMapping("/me")
    public ResponseEntity<ApiResponse<AuthResponse>> me(
            @RequestHeader(value = "Authorization", required = false) String authHeader) {

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.error("Non authentifié."));
        }

        String token = authHeader.substring(7);
        if (!jwtUtil.validateToken(token) || jwtUtil.isTokenExpired(token)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.error("Session expirée."));
        }

        String email = jwtUtil.extractEmail(token);
        Utilisateur utilisateur = utilisateurRepository.findByEmail(email).orElse(null);

        if (utilisateur == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.error("Utilisateur introuvable."));
        }

        AuthResponse authResponse = new AuthResponse(
                token,
                utilisateur.getId(),
                utilisateur.getNom(),
                utilisateur.getPrenom(),
                utilisateur.getEmail(),
                utilisateur.getRole().name());

        return ResponseEntity.ok(ApiResponse.ok("OK", authResponse));
    }

    // ---------- OUTILS PRIVÉS ----------
    private void envoyerOtp(Utilisateur utilisateur) {
        String otp = String.format("%06d", new Random().nextInt(999999));
        utilisateur.setOtpCode(otp);
        utilisateur.setOtpExpiry(LocalDateTime.now().plusMinutes(5));
        utilisateurRepository.save(utilisateur);

        emailService.envoyerEmail(utilisateur.getEmail(),
                "🔐 Code de vérification BankaGN - " + otp,
                "Bonjour " + utilisateur.getPrenom() + ",\n\n"
                        + "Votre code de vérification BankaGN est :\n\n"
                        + "╔══════════════╗\n║   " + otp + "   ║\n╚══════════════╝\n\n"
                        + "Ce code est valable 5 minutes.\n\nCordialement,\nL'équipe BankaGN");
    }

    private void enregistrerAudit(String action, String details,
                                  String effectuePar, JournalAudit.TypeAction typeAction) {
        journalAuditRepository.save(JournalAudit.builder()
                .action(action).details(details)
                .effectuePar(effectuePar).typeAction(typeAction).build());
    }

    private void notifierAdmin(String titre, String message) {
        Utilisateur admin = utilisateurRepository.findAll().stream()
                .filter(u -> u.getRole() == Utilisateur.Role.ADMIN)
                .findFirst().orElse(null);
        if (admin != null) {
            notificationRepository.save(Notification.builder()
                    .titre(titre).message(message)
                    .type(Notification.TypeNotification.SYSTEME)
                    .lu(false).utilisateur(admin).build());
        }
    }
}