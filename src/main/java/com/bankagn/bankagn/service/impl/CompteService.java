package com.bankagn.bankagn.service.impl;

import com.bankagn.bankagn.entity.Compte;
import com.bankagn.bankagn.entity.Utilisateur;
import com.bankagn.bankagn.repository.CompteRepository;
import com.bankagn.bankagn.repository.UtilisateurRepository;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.util.Base64;
import java.util.List;
import java.util.Random;

@Service
@RequiredArgsConstructor
public class CompteService {

    private final CompteRepository compteRepository;
    private final UtilisateurRepository utilisateurRepository;

    @Transactional
    public Compte creerCompte(String email,
                              String typeString) {
        Utilisateur utilisateur = utilisateurRepository
                .findByEmail(email).orElseThrow();

        Compte.TypeCompte type;
        if ("COURANT".equals(typeString)) {
            type = Compte.TypeCompte.COURANT;
        } else {
            type = Compte.TypeCompte.EPARGNE;
        }

        // Vérification doublon
        List<Compte> comptesExistants = compteRepository
                .findByUtilisateur(utilisateur);

        boolean dejaExiste = comptesExistants.stream()
                .anyMatch(c -> c.getType() == type);

        if (dejaExiste) {
            throw new RuntimeException(
                    "Vous avez déjà un compte " +
                            typeString + " ! Un seul compte " +
                            typeString + " est autorisé par client.");
        }

        String numeroCompte = genererNumeroCompte();
        BigDecimal taux = type == Compte.TypeCompte.EPARGNE
                ? new BigDecimal("3.5") : BigDecimal.ZERO;

        Compte compte = Compte.builder()
                .numeroCompte(numeroCompte)
                .type(type)
                .solde(BigDecimal.ZERO)
                .tauxInteret(taux)
                .statut(Compte.StatutCompte.ACTIF)
                .utilisateur(utilisateur)
                .build();

        // Générer QR Code
        try {
            QRCodeWriter qrCodeWriter = new QRCodeWriter();
            BitMatrix bitMatrix = qrCodeWriter.encode(
                    "BankaGN|" + numeroCompte + "|" + type,
                    BarcodeFormat.QR_CODE, 200, 200);
            ByteArrayOutputStream pngOutputStream =
                    new ByteArrayOutputStream();
            MatrixToImageWriter.writeToStream(
                    bitMatrix, "PNG", pngOutputStream);
            String qrBase64 = Base64.getEncoder()
                    .encodeToString(
                            pngOutputStream.toByteArray());
            compte.setQrCode(qrBase64);
        } catch (Exception e) {
            // QR Code optionnel
        }

        return compteRepository.save(compte);
    }

    public List<Compte> getComptesByEmail(String email) {
        Utilisateur utilisateur = utilisateurRepository
                .findByEmail(email).orElseThrow();
        return compteRepository.findByUtilisateur(utilisateur);
    }

    public Compte getCompteById(Long id) {
        return compteRepository.findById(id).orElseThrow();
    }

    private String genererNumeroCompte() {
        String numero;
        do {
            Random random = new Random();
            numero = "GN-2026-" + String.format("%05d",
                    random.nextInt(99999));
        } while (compteRepository
                .existsByNumeroCompte(numero));
        return numero;
    }

    public List<Compte> getAllComptes() {
        return compteRepository.findAll();
    }

    @Transactional
    public void bloquerCompte(Long id) {
        Compte compte = compteRepository
                .findById(id).orElseThrow();
        compte.setStatut(Compte.StatutCompte.BLOQUE);
        compteRepository.save(compte);
    }

    @Transactional
    public void debloquerCompte(Long id) {
        Compte compte = compteRepository
                .findById(id).orElseThrow();
        compte.setStatut(Compte.StatutCompte.ACTIF);
        compteRepository.save(compte);
    }

    @Transactional
    public void fermerCompte(Long id) {
        Compte compte = compteRepository
                .findById(id).orElseThrow();
        compteRepository.delete(compte);
    }
}