package com.bankagn.bankagn.controller;

import com.bankagn.bankagn.entity.Notification;
import com.bankagn.bankagn.entity.Utilisateur;
import com.bankagn.bankagn.repository.NotificationRepository;
import com.bankagn.bankagn.repository.UtilisateurRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Controller
@RequestMapping("/client/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationRepository notificationRepository;
    private final UtilisateurRepository utilisateurRepository;

    @GetMapping
    public String notifications(Authentication authentication,
                                Model model) {
        String email = authentication.getName();
        Utilisateur utilisateur = utilisateurRepository
                .findByEmail(email).orElseThrow();

        List<Notification> notifications =
                notificationRepository
                        .findByUtilisateurOrderByDateCreationDesc(
                                utilisateur);

        // Marquer toutes comme lues
        notifications.forEach(n -> n.setLu(true));
        notificationRepository.saveAll(notifications);

        model.addAttribute("notifications", notifications);
        return "client/notifications";
    }

    @GetMapping("/lire/{id}")
    public String lireNotification(
            @PathVariable Long id,
            RedirectAttributes redirectAttributes) {
        notificationRepository.findById(id)
                .ifPresent(n -> {
                    n.setLu(true);
                    notificationRepository.save(n);
                });
        return "redirect:/client/notifications";
    }
}