package com.bankagn.bankagn.repository;

import com.bankagn.bankagn.entity.Notification;
import com.bankagn.bankagn.entity.Utilisateur;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface NotificationRepository
        extends JpaRepository<Notification, Long> {

    List<Notification> findByUtilisateurOrderByDateCreationDesc(
            Utilisateur utilisateur);

    List<Notification> findByUtilisateurAndLu(
            Utilisateur utilisateur, boolean lu);

    long countByUtilisateurAndLu(
            Utilisateur utilisateur, boolean lu);
}