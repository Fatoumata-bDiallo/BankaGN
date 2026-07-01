package com.bankagn.bankagn.controller;

import com.bankagn.bankagn.entity.JournalAudit;
import com.bankagn.bankagn.entity.TauxDevise;
import com.bankagn.bankagn.repository.JournalAuditRepository;
import com.bankagn.bankagn.repository.TauxDeviseRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import jakarta.annotation.PostConstruct;
import java.math.BigDecimal;
import java.util.List;

@Controller
@RequestMapping("/admin/devises")
@RequiredArgsConstructor
public class TauxDeviseController {

    private final TauxDeviseRepository tauxDeviseRepository;
    private final JournalAuditRepository journalAuditRepository;

    @PostConstruct
    public void initialiserTaux() {
        if (tauxDeviseRepository.count() == 0) {
            tauxDeviseRepository.saveAll(List.of(
                    TauxDevise.builder()
                            .code("USD")
                            .nom("Dollar américain")
                            .drapeau("🇺🇸")
                            .tauxVersGNF(new BigDecimal("8695.65"))
                            .build(),
                    TauxDevise.builder()
                            .code("EUR")
                            .nom("Euro")
                            .drapeau("🇪🇺")
                            .tauxVersGNF(new BigDecimal("9433.96"))
                            .build(),
                    TauxDevise.builder()
                            .code("GBP")
                            .nom("Livre Sterling")
                            .drapeau("🇬🇧")
                            .tauxVersGNF(new BigDecimal("10989.01"))
                            .build(),
                    TauxDevise.builder()
                            .code("MAD")
                            .nom("Dirham marocain")
                            .drapeau("🇲🇦")
                            .tauxVersGNF(new BigDecimal("869.57"))
                            .build(),
                    TauxDevise.builder()
                            .code("XOF")
                            .nom("Franc CFA")
                            .drapeau("🌍")
                            .tauxVersGNF(new BigDecimal("1.44"))
                            .build()
            ));
        }
    }

    @GetMapping
    public String devises(Model model) {
        model.addAttribute("taux",
                tauxDeviseRepository.findAll());
        return "admin/devises";
    }

    @PostMapping("/modifier/{id}")
    public String modifierTaux(
            @PathVariable Long id,
            @RequestParam BigDecimal tauxVersGNF,
            Authentication authentication,
            RedirectAttributes redirectAttributes) {
        try {
            TauxDevise taux = tauxDeviseRepository
                    .findById(id).orElseThrow();

            BigDecimal ancienTaux = taux.getTauxVersGNF();
            taux.setTauxVersGNF(tauxVersGNF);
            tauxDeviseRepository.save(taux);

            // Audit
            JournalAudit journal = JournalAudit.builder()
                    .action("Taux de change modifié")
                    .details("Taux " + taux.getCode() +
                            " modifié de " + ancienTaux +
                            " GNF → " + tauxVersGNF + " GNF")
                    .effectuePar(authentication.getName())
                    .typeAction(JournalAudit.TypeAction.SYSTEME)
                    .build();
            journalAuditRepository.save(journal);

            redirectAttributes.addFlashAttribute("succes",
                    "✅ Taux " + taux.getCode() +
                            " mis à jour : " + tauxVersGNF + " GNF !");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("erreur",
                    "⚠️ Erreur lors de la mise à jour !");
        }
        return "redirect:/admin/devises";
    }
}