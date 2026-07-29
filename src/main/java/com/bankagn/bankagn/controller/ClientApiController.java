package com.bankagn.bankagn.controller;
import com.bankagn.bankagn.dto.DepotRetraitRequest;
import com.bankagn.bankagn.dto.TransfertRequest;
import com.bankagn.bankagn.entity.Carte;
import com.bankagn.bankagn.entity.Pret;
import com.bankagn.bankagn.service.impl.CarteService;
import com.bankagn.bankagn.service.impl.PretService;
import com.bankagn.bankagn.service.impl.ReleveService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import java.math.BigDecimal;
import com.bankagn.bankagn.entity.Beneficiaire;
import com.bankagn.bankagn.entity.Notification;
import com.bankagn.bankagn.entity.TauxDevise;
import com.bankagn.bankagn.repository.BeneficiaireRepository;
import com.bankagn.bankagn.repository.CompteRepository;
import com.bankagn.bankagn.repository.TauxDeviseRepository;
import com.bankagn.bankagn.dto.DashboardResponse;
import com.bankagn.bankagn.entity.Compte;
import com.bankagn.bankagn.entity.Transaction;
import com.bankagn.bankagn.entity.Utilisateur;
import com.bankagn.bankagn.repository.NotificationRepository;
import com.bankagn.bankagn.repository.UtilisateurRepository;
import com.bankagn.bankagn.service.impl.CompteService;
import com.bankagn.bankagn.service.impl.TransactionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/client")
@RequiredArgsConstructor
public class ClientApiController {

    private final UtilisateurRepository utilisateurRepository;
    private final CompteService compteService;
    private final TransactionService transactionService;
    private final NotificationRepository notificationRepository;
    private final PretService pretService;
    private final CarteService carteService;
    private final ReleveService releveService;
    private final PasswordEncoder passwordEncoder;
    private final BeneficiaireRepository beneficiaireRepository;
    private final CompteRepository compteRepository;
    private final TauxDeviseRepository tauxDeviseRepository;

    @GetMapping("/dashboard")
    public ResponseEntity<DashboardResponse> dashboard(
            Authentication authentication) {

        String email = authentication.getName();
        Utilisateur utilisateur = utilisateurRepository
                .findByEmail(email).orElseThrow();

        List<Compte> comptes = compteService.getComptesByEmail(email);

        BigDecimal soldeTotal = comptes.stream()
                .map(Compte::getSolde)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal soldeCourant = comptes.stream()
                .filter(c -> c.getType() == Compte.TypeCompte.COURANT)
                .map(Compte::getSolde)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal soldeEpargne = comptes.stream()
                .filter(c -> c.getType() == Compte.TypeCompte.EPARGNE)
                .map(Compte::getSolde)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        long notificationsNonLues = notificationRepository
                .countByUtilisateurAndLu(utilisateur, false);

        List<Transaction> transactions = transactionService
                .getDernieresTransactions(email);

        DashboardResponse response = new DashboardResponse(
                utilisateur.getPrenom(),
                utilisateur.getNom(),
                notificationsNonLues,
                comptes,
                soldeTotal,
                soldeCourant,
                soldeEpargne,
                transactions
        );

        return ResponseEntity.ok(response);
    }

    @PostMapping("/comptes/creer")
    public ResponseEntity<Map<String, String>> creerCompte(
            @RequestBody Map<String, String> body,
            Authentication authentication) {

        String email = authentication.getName();
        String type = body.get("type");

        try {
            compteService.creerCompte(email, type);
            return ResponseEntity.ok(
                    Map.of("message", "Compte " + type + " créé avec succès !"));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("message", e.getMessage()));
        }
    }
    @PostMapping("/depot")
    public ResponseEntity<Map<String, String>> depot(
            @RequestBody DepotRetraitRequest request) {
        try {
            transactionService.effectuerDepot(
                    request.getCompteId(),
                    request.getMontant(),
                    request.getDescription());
            return ResponseEntity.ok(
                    Map.of("message", "Dépôt effectué avec succès !"));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("message", e.getMessage()));
        }
    }

    @PostMapping("/retrait")
    public ResponseEntity<Map<String, String>> retrait(
            @RequestBody DepotRetraitRequest request) {
        try {
            transactionService.effectuerRetrait(
                    request.getCompteId(),
                    request.getMontant(),
                    request.getDescription());
            return ResponseEntity.ok(
                    Map.of("message", "Retrait effectué avec succès !"));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("message", e.getMessage()));
        }
    }

    @PostMapping("/transfert")
    public ResponseEntity<Map<String, String>> transfert(
            @RequestBody TransfertRequest request) {
        try {
            transactionService.effectuerTransfert(
                    request.getCompteSourceId(),
                    request.getNumeroDestination(),
                    request.getMontant(),
                    request.getDescription());
            return ResponseEntity.ok(
                    Map.of("message", "Transfert effectué avec succès !"));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("message", e.getMessage()));
        }
    }
    @GetMapping("/transactions")
    public ResponseEntity<List<Transaction>> transactions(
            Authentication authentication) {
        return ResponseEntity.ok(
                transactionService.getTransactionsByEmail(authentication.getName()));
    }

    @GetMapping("/releve")
    public ResponseEntity<byte[]> releve(Authentication authentication) {
        try {
            byte[] pdf = releveService.genererReleve(authentication.getName());
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_PDF);
            headers.setContentDispositionFormData(
                    "attachment", "releve-bankagn.pdf");
            return ResponseEntity.ok().headers(headers).body(pdf);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/prets")
    public ResponseEntity<List<Pret>> prets(Authentication authentication) {
        return ResponseEntity.ok(
                pretService.getPretsByEmail(authentication.getName()));
    }

    @PostMapping("/prets/demander")
    public ResponseEntity<Map<String, String>> demanderPret(
            @RequestBody Map<String, String> body,
            Authentication authentication) {
        try {
            pretService.demanderPret(
                    authentication.getName(),
                    new BigDecimal(body.get("montant")),
                    Integer.parseInt(body.get("dureeMois")),
                    new BigDecimal("8"),
                    body.get("motif"),
                    null);
            return ResponseEntity.ok(
                    Map.of("message", "Demande de prêt envoyée avec succès !"));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("message", e.getMessage()));
        }
    }

    @GetMapping("/cartes")
    public ResponseEntity<List<Carte>> cartes(Authentication authentication) {
        return ResponseEntity.ok(
                carteService.getCartesByEmail(authentication.getName()));
    }

    @PostMapping("/cartes/demander")
    public ResponseEntity<Map<String, String>> demanderCarte(
            @RequestBody Map<String, String> body) {
        try {
            Long compteId = Long.parseLong(body.get("compteId"));
            Carte.TypeCarte type = Carte.TypeCarte.valueOf(body.get("type"));
            carteService.creerCarte(compteId, type);
            return ResponseEntity.ok(
                    Map.of("message", "Carte créée avec succès !"));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("message", e.getMessage()));
        }
    }

    @GetMapping("/profil")
    public ResponseEntity<Utilisateur> profil(Authentication authentication) {
        Utilisateur utilisateur = utilisateurRepository
                .findByEmail(authentication.getName()).orElseThrow();
        return ResponseEntity.ok(utilisateur);
    }

    @PostMapping("/modifier-mdp")
    public ResponseEntity<Map<String, Object>> modifierMotDePasse(
            @RequestBody Map<String, String> body,
            Authentication authentication) {

        Utilisateur utilisateur = utilisateurRepository
                .findByEmail(authentication.getName()).orElseThrow();

        String ancien = body.get("ancienMotDePasse");
        String nouveau = body.get("nouveauMotDePasse");

        if (!passwordEncoder.matches(ancien, utilisateur.getMotDePasse())) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("success", false,
                            "message", "Ancien mot de passe incorrect !"));
        }

        utilisateur.setMotDePasse(passwordEncoder.encode(nouveau));
        utilisateurRepository.save(utilisateur);

        return ResponseEntity.ok(
                Map.of("success", true, "message", "Mot de passe modifié !"));
    }
    @GetMapping("/beneficiaires")
    public ResponseEntity<List<Beneficiaire>> beneficiaires(
            Authentication authentication) {
        Utilisateur utilisateur = utilisateurRepository
                .findByEmail(authentication.getName()).orElseThrow();
        return ResponseEntity.ok(
                beneficiaireRepository.findByUtilisateur(utilisateur));
    }

    @PostMapping("/beneficiaires/ajouter")
    public ResponseEntity<Map<String, String>> ajouterBeneficiaire(
            @RequestBody Map<String, String> body,
            Authentication authentication) {

        Utilisateur utilisateur = utilisateurRepository
                .findByEmail(authentication.getName()).orElseThrow();
        String numeroCompte = body.get("numeroCompte");

        if (!compteRepository.existsByNumeroCompte(numeroCompte)) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("message",
                            "Le numéro de compte " + numeroCompte + " n'existe pas !"));
        }
        if (beneficiaireRepository.existsByUtilisateurAndNumeroCompte(
                utilisateur, numeroCompte)) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("message",
                            "Ce bénéficiaire est déjà dans votre liste !"));
        }

        beneficiaireRepository.save(Beneficiaire.builder()
                .nom(body.get("nom"))
                .numeroCompte(numeroCompte)
                .telephone(body.get("telephone"))
                .description(body.get("description"))
                .utilisateur(utilisateur)
                .build());

        return ResponseEntity.ok(
                Map.of("message", "Bénéficiaire ajouté avec succès !"));
    }

    @DeleteMapping("/beneficiaires/{id}")
    public ResponseEntity<Map<String, String>> supprimerBeneficiaire(
            @PathVariable Long id) {
        beneficiaireRepository.deleteById(id);
        return ResponseEntity.ok(Map.of("message", "Bénéficiaire supprimé !"));
    }

    @GetMapping("/devises")
    public ResponseEntity<List<TauxDevise>> devises() {
        return ResponseEntity.ok(tauxDeviseRepository.findAll());
    }

    @GetMapping("/notifications")
    public ResponseEntity<List<Notification>> notifications(
            Authentication authentication) {
        Utilisateur utilisateur = utilisateurRepository
                .findByEmail(authentication.getName()).orElseThrow();
        List<Notification> notifications = notificationRepository
                .findByUtilisateurOrderByDateCreationDesc(utilisateur);
        notifications.forEach(n -> n.setLu(true));
        notificationRepository.saveAll(notifications);
        return ResponseEntity.ok(notifications);
    }

}