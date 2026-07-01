package com.bankagn.bankagn.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.HashMap;
import java.util.Map;

@Controller
@RequestMapping("/client/devises")
@RequiredArgsConstructor
public class DeviseController {

    // Taux de change fixes (base GNF)
    private static final Map<String, BigDecimal> TAUX =
            new HashMap<>();

    static {
        TAUX.put("GNF", BigDecimal.ONE);
        TAUX.put("USD", new BigDecimal("0.000115"));
        TAUX.put("EUR", new BigDecimal("0.000106"));
        TAUX.put("GBP", new BigDecimal("0.000091"));
        TAUX.put("MAD", new BigDecimal("0.00115"));
        TAUX.put("XOF", new BigDecimal("0.695"));
    }

    @GetMapping
    public String devisesPage(Model model) {
        model.addAttribute("taux", TAUX);
        model.addAttribute("resultat", null);
        return "client/devises";
    }

    @PostMapping("/convertir")
    public String convertir(
            @RequestParam BigDecimal montant,
            @RequestParam String deviseSource,
            @RequestParam String devisesCible,
            Model model) {

        // Convertir en GNF d'abord
        BigDecimal montantGNF = montant
                .divide(TAUX.get(deviseSource), 2,
                        RoundingMode.HALF_UP);

        // Convertir en devise cible
        BigDecimal resultat = montantGNF
                .multiply(TAUX.get(devisesCible))
                .setScale(2, RoundingMode.HALF_UP);

        model.addAttribute("taux", TAUX);
        model.addAttribute("montant", montant);
        model.addAttribute("deviseSource", deviseSource);
        model.addAttribute("devisesCible", devisesCible);
        model.addAttribute("resultat", resultat);

        return "client/devises";
    }
}