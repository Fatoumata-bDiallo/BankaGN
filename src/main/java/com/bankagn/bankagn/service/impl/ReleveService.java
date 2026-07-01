package com.bankagn.bankagn.service.impl;

import com.bankagn.bankagn.entity.Compte;
import com.bankagn.bankagn.entity.Transaction;
import com.bankagn.bankagn.entity.Utilisateur;
import com.bankagn.bankagn.repository.CompteRepository;
import com.bankagn.bankagn.repository.TransactionRepository;
import com.bankagn.bankagn.repository.UtilisateurRepository;
import com.lowagie.text.*;
import com.lowagie.text.Font;
import com.lowagie.text.Rectangle;
import com.lowagie.text.pdf.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.awt.*;
import java.io.ByteArrayOutputStream;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ReleveService {

    private final UtilisateurRepository utilisateurRepository;
    private final CompteRepository compteRepository;
    private final TransactionRepository transactionRepository;

    public byte[] genererReleve(String email) throws Exception {

        Utilisateur utilisateur = utilisateurRepository
                .findByEmail(email).orElseThrow();
        List<Compte> comptes = compteRepository
                .findByUtilisateur(utilisateur);

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        Document document = new Document(PageSize.A4, 40, 40, 60, 40);
        PdfWriter.getInstance(document, baos);
        document.open();

        // Couleurs
        Color bleu = new Color(26, 60, 94);
        Color or = new Color(240, 165, 0);
        Color gris = new Color(108, 117, 125);

        // Fonts
        Font fontTitre = new Font(Font.HELVETICA, 22,
                Font.BOLD, bleu);
        Font fontSousTitre = new Font(Font.HELVETICA, 11,
                Font.NORMAL, gris);
        Font fontSection = new Font(Font.HELVETICA, 13,
                Font.BOLD, bleu);
        Font fontNormal = new Font(Font.HELVETICA, 10,
                Font.NORMAL, Color.BLACK);
        Font fontBold = new Font(Font.HELVETICA, 10,
                Font.BOLD, Color.BLACK);
        Font fontWhite = new Font(Font.HELVETICA, 10,
                Font.BOLD, Color.WHITE);

        // ===== HEADER =====
        PdfPTable header = new PdfPTable(2);
        header.setWidthPercentage(100);
        header.setWidths(new float[]{1, 1});
        header.setSpacingAfter(20);

        // Logo BankaGN
        PdfPCell cellLogo = new PdfPCell();
        cellLogo.setBorder(Rectangle.NO_BORDER);
        cellLogo.setPadding(10);
        Paragraph logo = new Paragraph("BankaGN",
                new Font(Font.HELVETICA, 28, Font.BOLD, bleu));
        Paragraph slogan = new Paragraph(
                "Votre Banque Numérique en Guinée",
                fontSousTitre);
        cellLogo.addElement(logo);
        cellLogo.addElement(slogan);
        header.addCell(cellLogo);

        // Infos relevé
        PdfPCell cellInfo = new PdfPCell();
        cellInfo.setBorder(Rectangle.NO_BORDER);
        cellInfo.setPadding(10);
        cellInfo.setHorizontalAlignment(Element.ALIGN_RIGHT);
        Paragraph typeDoc = new Paragraph("RELEVÉ DE COMPTE",
                new Font(Font.HELVETICA, 14, Font.BOLD, or));
        typeDoc.setAlignment(Element.ALIGN_RIGHT);
        Paragraph dateDoc = new Paragraph(
                "Généré le : " + LocalDateTime.now()
                        .format(DateTimeFormatter.ofPattern(
                                "dd/MM/yyyy à HH:mm")),
                fontSousTitre);
        dateDoc.setAlignment(Element.ALIGN_RIGHT);
        cellInfo.addElement(typeDoc);
        cellInfo.addElement(dateDoc);
        header.addCell(cellInfo);
        document.add(header);

        // Ligne séparatrice
        PdfPTable ligne = new PdfPTable(1);
        ligne.setWidthPercentage(100);
        ligne.setSpacingAfter(20);
        PdfPCell ligneCel = new PdfPCell();
        ligneCel.setBackgroundColor(bleu);
        ligneCel.setFixedHeight(3);
        ligneCel.setBorder(Rectangle.NO_BORDER);
        ligne.addCell(ligneCel);
        document.add(ligne);

        // ===== INFOS CLIENT =====
        PdfPTable clientTable = new PdfPTable(2);
        clientTable.setWidthPercentage(100);
        clientTable.setSpacingAfter(25);

        PdfPCell clientCell = new PdfPCell();
        clientCell.setBorder(Rectangle.BOX);
        clientCell.setBorderColor(new Color(230, 230, 230));
        clientCell.setPadding(15);
        clientCell.setBackgroundColor(new Color(248, 249, 250));

        Paragraph titreClient = new Paragraph(
                "INFORMATIONS CLIENT", fontSection);
        titreClient.setSpacingAfter(8);
        clientCell.addElement(titreClient);
        clientCell.addElement(new Paragraph(
                "Nom : " + utilisateur.getPrenom()
                        + " " + utilisateur.getNom(), fontBold));
        clientCell.addElement(new Paragraph(
                "Email : " + utilisateur.getEmail(),
                fontNormal));
        clientCell.addElement(new Paragraph(
                "Téléphone : " + utilisateur.getTelephone(),
                fontNormal));
        clientTable.addCell(clientCell);

        PdfPCell compteCell = new PdfPCell();
        compteCell.setBorder(Rectangle.BOX);
        compteCell.setBorderColor(new Color(230, 230, 230));
        compteCell.setPadding(15);

        Paragraph titreCompte = new Paragraph(
                "MES COMPTES", fontSection);
        titreCompte.setSpacingAfter(8);
        compteCell.addElement(titreCompte);

        for (Compte c : comptes) {
            compteCell.addElement(new Paragraph(
                    "● " + c.getNumeroCompte() +
                            " (" + c.getType() + ") → " +
                            c.getSolde() + " GNF", fontNormal));
        }
        clientTable.addCell(compteCell);
        document.add(clientTable);

        // ===== TRANSACTIONS =====
        Paragraph titreTransactions = new Paragraph(
                "HISTORIQUE DES TRANSACTIONS", fontSection);
        titreTransactions.setSpacingAfter(10);
        document.add(titreTransactions);

        PdfPTable table = new PdfPTable(5);
        table.setWidthPercentage(100);
        table.setWidths(new float[]{1.5f, 2f, 1f, 1.5f, 1f});
        table.setSpacingAfter(20);

        // En-têtes tableau
        String[] headers = {"Date", "Description",
                "Type", "Montant", "Statut"};
        for (String h : headers) {
            PdfPCell hCell = new PdfPCell(
                    new Phrase(h, fontWhite));
            hCell.setBackgroundColor(bleu);
            hCell.setPadding(8);
            hCell.setHorizontalAlignment(Element.ALIGN_CENTER);
            hCell.setBorder(Rectangle.NO_BORDER);
            table.addCell(hCell);
        }

        // Données transactions
        List<Transaction> toutes = new ArrayList<>();
        for (Compte compte : comptes) {
            toutes.addAll(transactionRepository
                    .findByCompteSourceOrCompteDestinationOrderByDateTransactionDesc(
                            compte, compte));
        }

        boolean alt = false;
        for (Transaction t : toutes) {
            Color bg = alt ? new Color(248, 249, 250)
                    : Color.WHITE;
            alt = !alt;

            // Date
            PdfPCell dateCell = new PdfPCell(new Phrase(
                    t.getDateTransaction().format(
                            DateTimeFormatter.ofPattern(
                                    "dd/MM/yy HH:mm")), fontNormal));
            dateCell.setPadding(7);
            dateCell.setBackgroundColor(bg);
            dateCell.setBorderColor(new Color(230, 230, 230));
            table.addCell(dateCell);

            // Description
            PdfPCell descCell = new PdfPCell(new Phrase(
                    t.getDescription() != null ?
                            t.getDescription() : "-", fontNormal));
            descCell.setPadding(7);
            descCell.setBackgroundColor(bg);
            descCell.setBorderColor(new Color(230, 230, 230));
            table.addCell(descCell);

            // Type
            PdfPCell typeCell = new PdfPCell(new Phrase(
                    t.getType().name(), fontNormal));
            typeCell.setPadding(7);
            typeCell.setHorizontalAlignment(
                    Element.ALIGN_CENTER);
            typeCell.setBackgroundColor(bg);
            typeCell.setBorderColor(new Color(230, 230, 230));
            table.addCell(typeCell);

            // Montant
            Color montantColor = t.getType() ==
                    Transaction.TypeTransaction.DEPOT ?
                    new Color(40, 167, 69) :
                    new Color(220, 53, 69);
            String signe = t.getType() ==
                    Transaction.TypeTransaction.DEPOT ?
                    "+" : "-";
            PdfPCell montantCell = new PdfPCell(new Phrase(
                    signe + t.getMontant() + " GNF",
                    new Font(Font.HELVETICA, 10,
                            Font.BOLD, montantColor)));
            montantCell.setPadding(7);
            montantCell.setHorizontalAlignment(
                    Element.ALIGN_RIGHT);
            montantCell.setBackgroundColor(bg);
            montantCell.setBorderColor(new Color(230, 230, 230));
            table.addCell(montantCell);

            // Statut
            PdfPCell statutCell = new PdfPCell(new Phrase(
                    t.getStatut().name(), fontNormal));
            statutCell.setPadding(7);
            statutCell.setHorizontalAlignment(
                    Element.ALIGN_CENTER);
            statutCell.setBackgroundColor(bg);
            statutCell.setBorderColor(new Color(230, 230, 230));
            table.addCell(statutCell);
        }
        document.add(table);

        // ===== FOOTER =====
        PdfPTable footer = new PdfPTable(1);
        footer.setWidthPercentage(100);
        PdfPCell footerCell = new PdfPCell();
        footerCell.setBackgroundColor(bleu);
        footerCell.setBorder(Rectangle.NO_BORDER);
        footerCell.setPadding(12);
        Paragraph footerText = new Paragraph(
                "BankaGN - Votre Banque Numérique en Guinée  |  " +
                        "contact@bankagn.com  |  +224 626 335 841  |  " +
                        "Kaloum, Conakry, Guinée",
                new Font(Font.HELVETICA, 9,
                        Font.NORMAL, Color.WHITE));
        footerText.setAlignment(Element.ALIGN_CENTER);
        footerCell.addElement(footerText);
        footer.addCell(footerCell);
        document.add(footer);

        document.close();
        return baos.toByteArray();
    }
}