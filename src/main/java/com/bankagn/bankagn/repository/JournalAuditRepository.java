package com.bankagn.bankagn.repository;

import com.bankagn.bankagn.entity.JournalAudit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface JournalAuditRepository
        extends JpaRepository<JournalAudit, Long> {

    List<JournalAudit> findAllByOrderByDateActionDesc();

    List<JournalAudit> findByEffectueParOrderByDateActionDesc(
            String effectuePar);
}