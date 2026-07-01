package com.bankagn.bankagn.controller;

import com.bankagn.bankagn.service.impl.JournalAuditService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/admin/audit")
@RequiredArgsConstructor
public class AuditController {

    private final JournalAuditService journalAuditService;

    @GetMapping
    public String audit(Model model) {
        model.addAttribute("journaux",
                journalAuditService.getTout());
        return "admin/audit";
    }
}