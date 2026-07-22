package com.bankagn.bankagn.service.impl;

import com.bankagn.bankagn.entity.*;
import com.bankagn.bankagn.repository.*;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Random;

@Service
@RequiredArgsConstructor
public class TransactionService {

    private final TransactionRepository transactionRepository;
    private final CompteRepository compteRepository;
    private final NotificationRepository notificationRepository;
    private final UtilisateurRepository utilisateurRepository;
    private final AlerteFraudeRepository alerteFraudeRepository;
    private final JournalAuditRepository journalAuditRepository;

    private static final BigDecimal LIMITE_MONTANT_JOUR =
            new BigDecimal("10000000");
    private static final int LIMITE_TRANSACTIONS_JOUR = 10;

    // ===== GÉNÉRER QR CODE =====
    private String genererQrCode(Transaction transaction) {
        try {
            StringBuilder content = new StringBuilder();
            content.append("=== RECU BANKAGN ===\n");
            content.append("Ref : ").append(transaction.getReference()).append("\n");
            content.append("Type : ").append(transaction.getType()).append("\n");
            content.append("Montant : ").append(transaction.getMontant()).append(" GNF\n");
            content.append("Statut : ").append(transaction.getStatut()).append("\n");
            content.append("Date : ").append(LocalDateTime.now()
                    .format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"))).append("\n");
            if (transaction.getCompteSource() != null) {
                content.append("De : ").append(
                        transaction.getCompteSource()
                                .getNumeroCompte()).append("\n");
            }
            if (transaction.getCompteDestination() != null) {
                content.append("Vers : ").append(
                        transaction.getCompteDestination()
                                .getNumeroCompte()).append("\n");
            }
            content.append("=== BANKAGN 2026 ===");

            QRCodeWriter qrCodeWriter = new QRCodeWriter();
            BitMatrix bitMatrix = qrCodeWriter.encode(
                    content.toString(),
                    BarcodeFormat.QR_CODE, 200, 200);
            ByteArrayOutputStream pngOutputStream =
                    new ByteArrayOutputStream();
            MatrixToImageWriter.writeToStream(
                    bitMatrix, "PNG", pngOutputStream);
            return Base64.getEncoder().encodeToString(
                    pngOutputStream.toByteArray());
        } catch (Exception e) {
            return null;
        }
    }

    // ===== VÉRIFICATION LIMITES =====
    private void verifierLimites(Compte compte,
                                 BigDecimal montant) {
        LocalDateTime debutJour = LocalDate.now().atStartOfDay();
        LocalDateTime finJour = debutJour.plusDays(1);

        List<Transaction> transactionsJour =
                transactionRepository.findAll().stream()
                        .filter(t -> t.getCompteSource() != null
                                && t.getCompteSource().getId()
                                .equals(compte.getId())
                                && t.getDateTransaction() != null
                                && t.getDateTransaction().isAfter(debutJour)
                                && t.getDateTransaction().isBefore(finJour)
                                && t.getStatut() ==
                                Transaction.StatutTransaction.SUCCES)
                        .toList();

        if (transactionsJour.size() >= LIMITE_TRANSACTIONS_JOUR) {
            throw new RuntimeException(
                    "⚠️ Limite atteinte ! Vous avez effectué " +
                            LIMITE_TRANSACTIONS_JOUR +
                            " transactions aujourd'hui. " +
                            "Limite journalière atteinte.");
        }

        BigDecimal totalJour = transactionsJour.stream()
                .map(Transaction::getMontant)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal nouveauTotal = totalJour.add(montant);
        if (nouveauTotal.compareTo(LIMITE_MONTANT_JOUR) > 0) {
            BigDecimal restant = LIMITE_MONTANT_JOUR
                    .subtract(totalJour);
            throw new RuntimeException(
                    "⚠️ Limite journalière dépassée ! " +
                            "Vous avez déjà transféré " + totalJour +
                            " GNF aujourd'hui. " +
                            "Il vous reste " + restant +
                            " GNF disponibles pour aujourd'hui.");
        }
    }

    // ===== DÉPÔT =====
    @Transactional
    public Transaction effectuerDepot(Long compteId,
                                      BigDecimal montant,
                                      String description) {
        Compte compte = compteRepository
                .findById(compteId).orElseThrow();

        if (compte.getStatut() != Compte.StatutCompte.ACTIF) {
            throw new RuntimeException("Compte bloqué !");
        }

        if (montant.compareTo(new BigDecimal("1000")) < 0) {
            throw new RuntimeException(
                    "Montant minimum : 1 000 GNF !");
        }

        compte.setSolde(compte.getSolde().add(montant));
        compteRepository.save(compte);

        Transaction transaction = Transaction.builder()
                .reference(genererReference())
                .type(Transaction.TypeTransaction.DEPOT)
                .montant(montant)
                .description(description != null ?
                        description : "Dépôt bancaire")
                .statut(Transaction.StatutTransaction.SUCCES)
                .compteDestination(compte)
                .build();

        // Générer QR Code
        transaction.setQrCode(genererQrCode(transaction));
        transactionRepository.save(transaction);

        creerNotification(compte.getUtilisateur(),
                "Dépôt reçu",
                "Dépôt de " + montant + " GNF sur "
                        + compte.getNumeroCompte(),
                Notification.TypeNotification.DEPOT);

        enregistrerAudit("Dépôt effectué",
                "Dépôt de " + montant + " GNF sur "
                        + compte.getNumeroCompte(),
                compte.getUtilisateur().getEmail(),
                JournalAudit.TypeAction.TRANSACTION);

        return transaction;
    }

    // ===== RETRAIT =====
    @Transactional
    public Transaction effectuerRetrait(Long compteId,
                                        BigDecimal montant,
                                        String description) {
        Compte compte = compteRepository
                .findById(compteId).orElseThrow();

        if (compte.getStatut() != Compte.StatutCompte.ACTIF) {
            throw new RuntimeException("Compte bloqué !");
        }

        if (montant.compareTo(new BigDecimal("1000")) < 0) {
            throw new RuntimeException(
                    "Montant minimum : 1 000 GNF !");
        }

        if (compte.getSolde().compareTo(montant) < 0) {
            throw new RuntimeException("Solde insuffisant !");
        }

        verifierLimites(compte, montant);

        compte.setSolde(compte.getSolde().subtract(montant));
        compteRepository.save(compte);

        Transaction transaction = Transaction.builder()
                .reference(genererReference())
                .type(Transaction.TypeTransaction.RETRAIT)
                .montant(montant)
                .description(description != null ?
                        description : "Retrait bancaire")
                .statut(Transaction.StatutTransaction.SUCCES)
                .compteSource(compte)
                .build();

        // Générer QR Code
        transaction.setQrCode(genererQrCode(transaction));
        transactionRepository.save(transaction);

        creerNotification(compte.getUtilisateur(),
                "Retrait effectué",
                "Retrait de " + montant + " GNF sur "
                        + compte.getNumeroCompte(),
                Notification.TypeNotification.RETRAIT);

        enregistrerAudit("Retrait effectué",
                "Retrait de " + montant + " GNF sur "
                        + compte.getNumeroCompte(),
                compte.getUtilisateur().getEmail(),
                JournalAudit.TypeAction.TRANSACTION);

        if (montant.compareTo(new BigDecimal("5000000")) > 0) {
            creerAlerteFraude(compte.getUtilisateur(),
                    "Retrait suspect",
                    "Retrait inhabituel de " + montant +
                            " GNF sur " + compte.getNumeroCompte(),
                    AlerteFraude.NiveauAlerte.ELEVE);
        }

        verifierRetraitsRapides(compte);

        return transaction;
    }

    // ===== TRANSFERT =====
    @Transactional
    public Transaction effectuerTransfert(
            Long compteSourceId,
            String numeroDestination,
            BigDecimal montant,
            String description) {

        Compte compteSource = compteRepository
                .findById(compteSourceId).orElseThrow();

        Compte compteDestination = compteRepository
                .findByNumeroCompte(numeroDestination)
                .orElseThrow(() -> new RuntimeException(
                        "Compte destinataire introuvable !"));

        if (compteSource.getStatut() !=
                Compte.StatutCompte.ACTIF) {
            throw new RuntimeException("Compte source bloqué !");
        }

        if (montant.compareTo(new BigDecimal("1000")) < 0) {
            throw new RuntimeException(
                    "Montant minimum : 1 000 GNF !");
        }

        if (compteSource.getSolde().compareTo(montant) < 0) {
            throw new RuntimeException("Solde insuffisant !");
        }

        if (compteSource.getId().equals(
                compteDestination.getId())) {
            throw new RuntimeException(
                    "Impossible de transférer vers le même compte !");
        }

        verifierLimites(compteSource, montant);

        compteSource.setSolde(
                compteSource.getSolde().subtract(montant));
        compteDestination.setSolde(
                compteDestination.getSolde().add(montant));

        compteRepository.save(compteSource);
        compteRepository.save(compteDestination);

        Transaction transaction = Transaction.builder()
                .reference(genererReference())
                .type(Transaction.TypeTransaction.TRANSFERT)
                .montant(montant)
                .description(description != null ?
                        description : "Transfert bancaire")
                .statut(Transaction.StatutTransaction.SUCCES)
                .compteSource(compteSource)
                .compteDestination(compteDestination)
                .build();

        // Générer QR Code
        transaction.setQrCode(genererQrCode(transaction));
        transactionRepository.save(transaction);

        creerNotification(compteSource.getUtilisateur(),
                "Transfert envoyé",
                "Transfert de " + montant + " GNF vers "
                        + numeroDestination,
                Notification.TypeNotification.TRANSFERT);

        creerNotification(compteDestination.getUtilisateur(),
                "Transfert reçu",
                "Vous avez reçu " + montant + " GNF de "
                        + compteSource.getNumeroCompte(),
                Notification.TypeNotification.TRANSFERT);

        enregistrerAudit("Transfert effectué",
                "Transfert de " + montant + " GNF vers "
                        + numeroDestination,
                compteSource.getUtilisateur().getEmail(),
                JournalAudit.TypeAction.TRANSACTION);

        if (montant.compareTo(new BigDecimal("10000000")) > 0) {
            creerAlerteFraude(compteSource.getUtilisateur(),
                    "Transfert suspect",
                    "Transfert inhabituel de " + montant +
                            " GNF vers " + numeroDestination,
                    AlerteFraude.NiveauAlerte.MOYEN);
        }

        return transaction;
    }

    private void verifierRetraitsRapides(Compte compte) {
        LocalDateTime uneHeure = LocalDateTime.now()
                .minusHours(1);

        long nbRetraits = transactionRepository.findAll()
                .stream()
                .filter(t -> t.getType() ==
                        Transaction.TypeTransaction.RETRAIT
                        && t.getCompteSource() != null
                        && t.getCompteSource().getId()
                        .equals(compte.getId())
                        && t.getDateTransaction() != null
                        && t.getDateTransaction()
                        .isAfter(uneHeure))
                .count();

        if (nbRetraits >= 3) {
            creerAlerteFraude(compte.getUtilisateur(),
                    "Retraits multiples suspects",
                    nbRetraits + " retraits en moins d'1h sur "
                            + compte.getNumeroCompte(),
                    AlerteFraude.NiveauAlerte.ELEVE);
        }
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
                .utilisateur(utilisateur)
                .build();
        alerteFraudeRepository.save(alerte);

        Utilisateur admin = utilisateurRepository.findAll()
                .stream()
                .filter(u -> u.getRole() ==
                        Utilisateur.Role.ADMIN)
                .findFirst().orElse(null);

        if (admin != null) {
            creerNotification(admin,
                    "🚨 Alerte Fraude - " + niveau,
                    "Alerte : " + typeAlerte + " pour "
                            + utilisateur.getPrenom() + " "
                            + utilisateur.getNom(),
                    Notification.TypeNotification.ALERTE);
        }
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

    public List<Transaction> getTransactionsByEmail(
            String email) {
        Utilisateur utilisateur = utilisateurRepository
                .findByEmail(email).orElseThrow();
        List<Compte> comptes = compteRepository
                .findByUtilisateur(utilisateur);

        List<Transaction> toutes = new ArrayList<>();
        for (Compte compte : comptes) {
            toutes.addAll(transactionRepository
                    .findByCompteSourceOrCompteDestinationOrderByDateTransactionDesc(
                            compte, compte));
        }
        return toutes;
    }

    public List<Transaction> getDernieresTransactions(
            String email) {
        Utilisateur utilisateur = utilisateurRepository
                .findByEmail(email).orElseThrow();
        List<Compte> comptes = compteRepository
                .findByUtilisateur(utilisateur);

        List<Transaction> toutes = new ArrayList<>();
        for (Compte compte : comptes) {
            toutes.addAll(transactionRepository
                    .findTop5ByCompteSourceOrCompteDestinationOrderByDateTransactionDesc(
                            compte, compte));
        }
        return toutes.stream().limit(5).toList();
    }

    private String genererReference() {
        return "TXN-" + System.currentTimeMillis() +
                "-" + (new Random().nextInt(9000) + 1000);
    }

    private void creerNotification(Utilisateur utilisateur,
                                   String titre, String message,
                                   Notification.TypeNotification type) {
        Notification notification = Notification.builder()
                .titre(titre).message(message)
                .type(type).lu(false)
                .utilisateur(utilisateur).build();
        notificationRepository.save(notification);
    }
}