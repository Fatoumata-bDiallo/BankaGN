package com.bankagn.bankagn.security;

import com.bankagn.bankagn.entity.Utilisateur;
import com.bankagn.bankagn.repository.UtilisateurRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class CustomUserDetailsService implements UserDetailsService {

    private final UtilisateurRepository utilisateurRepository;

    @Override
    public UserDetails loadUserByUsername(String email)
            throws UsernameNotFoundException {

        Utilisateur utilisateur = utilisateurRepository
                .findByEmail(email)
                .orElseThrow(() ->
                        new UsernameNotFoundException(
                                "Utilisateur non trouvé : " + email));

        // Vérifier statut BLOQUE
        if (utilisateur.getStatut() ==
                Utilisateur.Statut.BLOQUE) {
            throw new LockedException("Compte bloqué !");
        }

        // Vérifier statut INACTIF
        if (utilisateur.getStatut() ==
                Utilisateur.Statut.INACTIF) {
            throw new DisabledException("Compte inactif !");
        }

        // Ne pas bloquer EN_ATTENTE ici
        // AuthController gère ce cas

        return User.builder()
                .username(utilisateur.getEmail())
                .password(utilisateur.getMotDePasse())
                .authorities(List.of(
                        new SimpleGrantedAuthority(
                                "ROLE_" + utilisateur.getRole()
                                        .name())))
                .build();
    }
}