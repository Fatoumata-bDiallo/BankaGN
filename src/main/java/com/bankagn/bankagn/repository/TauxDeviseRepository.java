package com.bankagn.bankagn.repository;

import com.bankagn.bankagn.entity.TauxDevise;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface TauxDeviseRepository
        extends JpaRepository<TauxDevise, Long> {

    Optional<TauxDevise> findByCode(String code);
}