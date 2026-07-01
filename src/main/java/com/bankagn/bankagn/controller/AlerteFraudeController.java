package com.bankagn.bankagn.controller;

import com.bankagn.bankagn.entity.AlerteFraude;
import com.bankagn.bankagn.repository.AlerteFraudeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Controller
@RequestMapping("/admin/fraudes")
@RequiredArgsConstructor
public class AlerteFraudeController {

    private final AlerteFraudeRepository alerteFraudeRepository;

    @GetMapping
    public String fraudes(Model model) {
        List<AlerteFraude> fraudes =
                alerteFraudeRepository.findAll();

        long enCours = fraudes.stream()
                .filter(f -> f.getStatut() ==
                        AlerteFraude.StatutAlerteEnum.EN_COURS)
                .count();
        long resolues = fraudes.stream()
                .filter(f -> f.getStatut() ==
                        AlerteFraude.StatutAlerteEnum.RESOLUE)
                .count();
        long ignorees = fraudes.stream()
                .filter(f -> f.getStatut() ==
                        AlerteFraude.StatutAlerteEnum.IGNOREE)
                .count();

        model.addAttribute("fraudes", fraudes);
        model.addAttribute("enCours", enCours);
        model.addAttribute("resolues", resolues);
        model.addAttribute("ignorees", ignorees);
        return "admin/fraudes";
    }

    @GetMapping("/resoudre/{id}")
    public String resoudre(
            @PathVariable Long id,
            RedirectAttributes redirectAttributes) {
        alerteFraudeRepository.findById(id)
                .ifPresent(a -> {
                    a.setStatut(
                            AlerteFraude.StatutAlerteEnum.RESOLUE);
                    alerteFraudeRepository.save(a);
                });
        redirectAttributes.addFlashAttribute("succes",
                "✅ Alerte résolue !");
        return "redirect:/admin/fraudes";
    }

    @GetMapping("/ignorer/{id}")
    public String ignorer(
            @PathVariable Long id,
            RedirectAttributes redirectAttributes) {
        alerteFraudeRepository.findById(id)
                .ifPresent(a -> {
                    a.setStatut(
                            AlerteFraude.StatutAlerteEnum.IGNOREE);
                    alerteFraudeRepository.save(a);
                });
        redirectAttributes.addFlashAttribute("succes",
                "Alerte ignorée !");
        return "redirect:/admin/fraudes";
    }
}