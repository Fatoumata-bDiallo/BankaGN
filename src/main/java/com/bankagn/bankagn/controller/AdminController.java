package com.bankagn.bankagn.controller;

import com.bankagn.bankagn.entity.AlerteFraude;
import com.bankagn.bankagn.entity.JournalAudit;
import com.bankagn.bankagn.entity.Notification;
import com.bankagn.bankagn.entity.Utilisateur;
import com.bankagn.bankagn.repository.AlerteFraudeRepository;
import com.bankagn.bankagn.repository.CompteRepository;
import com.bankagn.bankagn.repository.JournalAuditRepository;
import com.bankagn.bankagn.repository.NotificationRepository;
import com.bankagn.bankagn.repository.TransactionRepository;
import com.bankagn.bankagn.repository.UtilisateurRepository;
import com.bankagn.bankagn.service.impl.CompteService;
import com.bankagn.bankagn.service.impl.PretService;
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
@RequestMapping("/admin")
@RequiredArgsConstructor
public class AdminController {

    private final UtilisateurRepository utilisateurRepository;
    private final CompteRepository compteRepository;
    private final TransactionRepository transactionRepository;
    private final CompteService compteService;
    private final NotificationRepository notificationRepository;
    private final PretService pretService;
    private final AlerteFraudeRepository alerteFraudeRepository;
    private final JournalAuditRepository journalAuditRepository;

    @GetMapping("/dashboard")
    public String dashboard(Authentication authentication,
                            Model model) {
        String email = authentication.getName();
        Utilisateur admin = utilisateurRepository
                .findByEmail(email).orElseThrow();

        long totalClients = utilisateurRepository
                .countByRole(Utilisateur.Role.CLIENT);
        long totalComptes = compteRepository.count();
        long totalDepots = transactionRepository.findAll()
                .stream()
                .filter(t -> t.getType() ==
                        com.bankagn.bankagn.entity.Transaction
                                .TypeTransaction.DEPOT)
                .count();
        long totalRetraits = transactionRepository.findAll()
                .stream()
                .filter(t -> t.getType() ==
                        com.bankagn.bankagn.entity.Transaction
                                .TypeTransaction.RETRAIT)
                .count();
        long totalPrets = pretService.getAllPrets().size();
        long totalFraudes = alerteFraudeRepository
                .countByStatut(
                        AlerteFraude.StatutAlerteEnum.EN_COURS);

        long enAttente = utilisateurRepository.findAll()
                .stream()
                .filter(u -> u.getRole() ==
                        Utilisateur.Role.CLIENT &&
                        u.getStatut() ==
                                Utilisateur.Statut.EN_ATTENTE)
                .count();

        List<Utilisateur> dernierUtilisateurs =
                utilisateurRepository.findAll()
                        .stream()
                        .filter(u -> u.getRole() ==
                                Utilisateur.Role.CLIENT)
                        .limit(5)
                        .toList();

        long notificationsNonLues = notificationRepository
                .countByUtilisateurAndLu(admin, false);

        model.addAttribute("admin", admin);
        model.addAttribute("totalClients", totalClients);
        model.addAttribute("totalComptes", totalComptes);
        model.addAttribute("totalDepots", totalDepots);
        model.addAttribute("totalRetraits", totalRetraits);
        model.addAttribute("totalPrets", totalPrets);
        model.addAttribute("totalFraudes", totalFraudes);
        model.addAttribute("enAttente", enAttente);
        model.addAttribute("dernierUtilisateurs",
                dernierUtilisateurs);
        model.addAttribute("notificationsNonLues",
                notificationsNonLues);
        return "admin/dashboard";
    }

    @GetMapping("/utilisateurs")
    public String utilisateurs(Model model) {
        List<Utilisateur> utilisateurs =
                utilisateurRepository.findAll()
                        .stream()
                        .filter(u -> u.getRole() ==
                                Utilisateur.Role.CLIENT)
                        .toList();

        long enAttente = utilisateurs.stream()
                .filter(u -> u.getStatut() ==
                        Utilisateur.Statut.EN_ATTENTE)
                .count();

        model.addAttribute("utilisateurs", utilisateurs);
        model.addAttribute("enAttente", enAttente);
        return "admin/utilisateurs";
    }

    @GetMapping("/utilisateurs/valider/{id}")
    public String validerUtilisateur(
            @PathVariable Long id,
            Authentication authentication,
            RedirectAttributes redirectAttributes) {
        Utilisateur u = utilisateurRepository
                .findById(id).orElseThrow();
        u.setStatut(Utilisateur.Statut.ACTIF);
        utilisateurRepository.save(u);

        Notification notification = Notification.builder()
                .titre("✅ Compte activé !")
                .message("Félicitations " + u.getPrenom() +
                        " ! Votre compte BankaGN a été validé.")
                .type(Notification.TypeNotification.SYSTEME)
                .lu(false)
                .utilisateur(u)
                .build();
        notificationRepository.save(notification);

        // Audit
        enregistrerAudit(
                "Compte validé",
                "Compte de " + u.getPrenom() + " " +
                        u.getNom() + " (" + u.getEmail() + ") validé",
                authentication.getName(),
                JournalAudit.TypeAction.UTILISATEUR);

        redirectAttributes.addFlashAttribute("succes",
                "✅ Compte de " + u.getPrenom() +
                        " " + u.getNom() + " validé !");
        return "redirect:/admin/utilisateurs";
    }

    @GetMapping("/utilisateurs/refuser/{id}")
    public String refuserUtilisateur(
            @PathVariable Long id,
            Authentication authentication,
            RedirectAttributes redirectAttributes) {
        Utilisateur u = utilisateurRepository
                .findById(id).orElseThrow();
        u.setStatut(Utilisateur.Statut.INACTIF);
        utilisateurRepository.save(u);

        Notification notification = Notification.builder()
                .titre("❌ Inscription refusée")
                .message("Désolé " + u.getPrenom() +
                        ", votre inscription a été refusée.")
                .type(Notification.TypeNotification.SYSTEME)
                .lu(false)
                .utilisateur(u)
                .build();
        notificationRepository.save(notification);

        // Audit
        enregistrerAudit(
                "Inscription refusée",
                "Inscription de " + u.getPrenom() + " " +
                        u.getNom() + " (" + u.getEmail() + ") refusée",
                authentication.getName(),
                JournalAudit.TypeAction.UTILISATEUR);

        redirectAttributes.addFlashAttribute("succes",
                "❌ Inscription de " + u.getPrenom() +
                        " " + u.getNom() + " refusée !");
        return "redirect:/admin/utilisateurs";
    }

    @GetMapping("/utilisateurs/bloquer/{id}")
    public String bloquerUtilisateur(
            @PathVariable Long id,
            Authentication authentication,
            RedirectAttributes redirectAttributes) {
        Utilisateur u = utilisateurRepository
                .findById(id).orElseThrow();
        u.setStatut(Utilisateur.Statut.BLOQUE);
        utilisateurRepository.save(u);

        // Audit
        enregistrerAudit(
                "Utilisateur bloqué",
                "Compte de " + u.getPrenom() + " " +
                        u.getNom() + " bloqué",
                authentication.getName(),
                JournalAudit.TypeAction.UTILISATEUR);

        redirectAttributes.addFlashAttribute("succes",
                "🔒 Utilisateur bloqué !");
        return "redirect:/admin/utilisateurs";
    }

    @GetMapping("/utilisateurs/debloquer/{id}")
    public String debloquerUtilisateur(
            @PathVariable Long id,
            Authentication authentication,
            RedirectAttributes redirectAttributes) {
        Utilisateur u = utilisateurRepository
                .findById(id).orElseThrow();
        u.setStatut(Utilisateur.Statut.ACTIF);
        utilisateurRepository.save(u);

        // Audit
        enregistrerAudit(
                "Utilisateur débloqué",
                "Compte de " + u.getPrenom() + " " +
                        u.getNom() + " débloqué",
                authentication.getName(),
                JournalAudit.TypeAction.UTILISATEUR);

        redirectAttributes.addFlashAttribute("succes",
                "🔓 Utilisateur débloqué !");
        return "redirect:/admin/utilisateurs";
    }

    @GetMapping("/comptes")
    public String comptes(Model model) {
        model.addAttribute("comptes",
                compteRepository.findAll());
        return "admin/comptes";
    }

    @GetMapping("/comptes/bloquer/{id}")
    public String bloquerCompte(
            @PathVariable Long id,
            Authentication authentication,
            RedirectAttributes redirectAttributes) {
        compteService.bloquerCompte(id);

        // Audit
        enregistrerAudit(
                "Compte bancaire bloqué",
                "Compte ID " + id + " bloqué par admin",
                authentication.getName(),
                JournalAudit.TypeAction.COMPTE);

        redirectAttributes.addFlashAttribute("succes",
                "🔒 Compte bloqué !");
        return "redirect:/admin/comptes";
    }

    @GetMapping("/comptes/debloquer/{id}")
    public String debloquerCompte(
            @PathVariable Long id,
            Authentication authentication,
            RedirectAttributes redirectAttributes) {
        compteService.debloquerCompte(id);

        // Audit
        enregistrerAudit(
                "Compte bancaire débloqué",
                "Compte ID " + id + " débloqué par admin",
                authentication.getName(),
                JournalAudit.TypeAction.COMPTE);

        redirectAttributes.addFlashAttribute("succes",
                "🔓 Compte débloqué !");
        return "redirect:/admin/comptes";
    }

    @GetMapping("/transactions")
    public String transactions(Model model) {
        model.addAttribute("transactions",
                transactionRepository.findAll());
        return "admin/transactions";
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