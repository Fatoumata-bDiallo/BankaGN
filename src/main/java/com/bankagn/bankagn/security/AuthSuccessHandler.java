package com.bankagn.bankagn.security;

import com.bankagn.bankagn.entity.Utilisateur;
import com.bankagn.bankagn.repository.UtilisateurRepository;
import com.bankagn.bankagn.service.impl.EmailService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Random;

@Component
@RequiredArgsConstructor
public class AuthSuccessHandler
        implements AuthenticationSuccessHandler {

    private final UtilisateurRepository utilisateurRepository;
    private final EmailService emailService;

    @Override
    public void onAuthenticationSuccess(
            HttpServletRequest request,
            HttpServletResponse response,
            Authentication authentication)
            throws IOException {

        String email = authentication.getName();
        Utilisateur utilisateur = utilisateurRepository
                .findByEmail(email).orElse(null);

        if (utilisateur == null) {
            response.sendRedirect("/auth/login?error");
            return;
        }

        // Générer OTP
        String otp = String.format("%06d",
                new Random().nextInt(999999));
        utilisateur.setOtpCode(otp);
        utilisateur.setOtpExpiry(
                LocalDateTime.now().plusMinutes(5));
        utilisateurRepository.save(utilisateur);

        // Stocker email en session
        HttpSession session = request.getSession();
        session.setAttribute("otpEmail", email);

        // Envoyer OTP par email
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

        response.sendRedirect("/auth/otp");
    }
}