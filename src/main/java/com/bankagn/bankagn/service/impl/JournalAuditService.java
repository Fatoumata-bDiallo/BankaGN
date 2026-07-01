package com.bankagn.bankagn.service.impl;

import com.bankagn.bankagn.entity.JournalAudit;
import com.bankagn.bankagn.repository.JournalAuditRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class JournalAuditService {

    private final JournalAuditRepository journalAuditRepository;

    public void enregistrer(String action,
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

    public List<JournalAudit> getTout() {
        return journalAuditRepository
                .findAllByOrderByDateActionDesc();
    }
}